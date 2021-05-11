package datawave.ingest.table.aggregator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.accumulo.core.client.IteratorSetting;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;

import com.google.protobuf.InvalidProtocolBufferException;

import datawave.ingest.protobuf.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of an Aggregator that aggregates objects of the type Uid.List. This is an optimization for the shardIndex and shardReverseIndex, where the
 * list of UIDs for events will be maintained in the global index for low cardinality terms.
 */
public class GlobalIndexUidAggregator extends PropogatingCombiner {
    private static final Logger log = LoggerFactory.getLogger(GlobalIndexUidAggregator.class);
    private static final String MAX_UIDS_OPT = "maxuids";
    private static final String PROCESS_IN_TIMESTAMP_ORDER = "processInTimestampOrder";
    private Uid.List.Builder builder = Uid.List.newBuilder();
    
    /**
     * Using a set instead of a list so that duplicate UIDs are filtered out of the list. This might happen in the case of rows with masked fields that share a
     * UID.
     */
    private final HashSet<String> uids = new HashSet<>();
    
    /**
     * List of UIDs to remove.
     */
    private final HashSet<String> uidsToRemove = new HashSet<>();
    
    /**
     * List of UIDs to remove.
     */
    private final HashSet<String> quarantinedIds = new HashSet<>();
    
    /**
     * List of UIDs to remove.
     */
    private final HashSet<String> releasedUids = new HashSet<>();
    
    /**
     * flag for whether or not we have seen ignore
     */
    private boolean seenIgnore = false;
    
    /**
     * Maximum number of UIDs.
     */
    public static final int MAX = 20;
    
    /**
     * Maximum number of UIDs.
     */
    public int maxUids;
    
    private boolean processInTimestampOrder = false;
    
    /**
     * representative count.
     */
    private long count = 0;
    
    public GlobalIndexUidAggregator(int max) {
        this.maxUids = max;
    }
    
    public GlobalIndexUidAggregator() {
        this.maxUids = MAX;
    }
    
    public Value aggregate() {
        
        // as a backup, we remove the intersection of the UID sets
        
        builder.setCOUNT(count);
        
        if (seenIgnore || count > maxUids) {
            builder.setIGNORE(true);
            builder.clearUID();
            // if we catch seenIgnore, then there is
            // no need to propagate removals.
            propogate = false;
        } else {
            builder.setIGNORE(false);
            
            uidsToRemove.removeAll(quarantinedIds);
            uidsToRemove.removeAll(releasedUids);
            quarantinedIds.removeAll(releasedUids);
            
            uids.removeAll(uidsToRemove);
            uids.removeAll(quarantinedIds);
            
            if (!releasedUids.isEmpty()) {
                log.debug("Adding released UIDS");
                uids.addAll(releasedUids);
            }
            
            builder.addAllUID(uids);
        }
        
        log.debug("Propagating: {}", propogate);
        
        // clear all removals
        builder.clearREMOVEDUID();
        
        if (propogate) {
            
            builder.addAllREMOVEDUID(uidsToRemove);
            builder.addAllQUARANTINEUID(quarantinedIds);
        }
        log.debug("Building aggregate. Count is {}, uids.size() is {}. builder size is {}", count, uids.size(), builder.getUIDList().size());
        return new Value(builder.build().toByteArray());
        
    }
    
    /**
     * We should closely examine the possible use cases to ensure that we have covered all scenarios.
     * 
     * Ingest: If we ingest, we would like to aggregate index entries with the same Key. This means that the reducer ( or combiner ) will combine UIDs for a
     * given index ( on a given shard ). In this case it is unlikely that we have any removals.
     * 
     * Deletes: We may have have removals at any point in the RFile read for a given tablet. We need to propagate the removals across compactions, until we have
     * a full major compaction.
     * 
     * If we reach the point where we are merging a UID protobuf, where ignore has been seen, then we do not continue with removals.
     */
    @Override
    public Value reduce(Key key, Iterator<Value> iter) {
        if (log.isTraceEnabled())
            log.trace("has next ? {}", iter.hasNext());
        while (iter.hasNext()) {
            
            Value value = iter.next();
            
            // Collect the values, which are serialized Uid.List objects
            try {
                Uid.List v = Uid.List.parseFrom(value.get());
                
                long delta = v.getCOUNT();
                
                count += delta;
                /*
                 * Fail fast approach.
                 */
                if (v.getIGNORE()) {
                    seenIgnore = true;
                    log.debug("SeenIgnore is true. Skipping collections");
                }
                
                // if delta > 0, we are collecting the uid list
                // in the protobuf into our object's uid list.
                if (delta > 0) {
                    
                    for (String uid : v.getQUARANTINEUIDList()) {
                        
                        quarantinedIds.remove(uid);
                        releasedUids.add(uid);
                    }
                    
                    for (String uid : v.getUIDList()) {
                        // check that a removal has not occurred
                        // if it has, we decrement the count, from above.
                        if (!uidsToRemove.contains(uid) && !quarantinedIds.contains(uid)) {
                            
                            // add the UID iff we are under our MAX
                            if (uids.size() < maxUids)
                                uids.add(uid);
                        }
                    }
                    
                    // It's possible due to previous compactions that we have a positive count as well as UIDs in the removed list.
                    // Add UIDs that aren't already in the UIDs list (e.g, the UID was added back and we saw it in an earlier
                    // key (which is a newer key with a larger timestamp value).
                    if (!seenIgnore && processInTimestampOrder) {
                        for (String uid : v.getREMOVEDUIDList()) {
                            if (!uids.contains(uid)) {
                                uidsToRemove.add(uid);
                            }
                        }
                    }
                    
                    log.debug("Adding uids {} {}", delta, count);
                    
                    // if our delta is < 0, then we can remove, iff seenIgnore is false. If it is true, there is no need to proceed with removals
                } else if (delta < 0 && !seenIgnore) {
                    
                    // so that we can perform the decrement
                    for (String uid : v.getREMOVEDUIDList()) {
                        
                        // If we're processing in timestamp order and UIDs contains the removed UID, that means
                        // a newer key (larger timestamp value) added the UID back in and we don't want to remove
                        // it here. If we're not processing in timestamp order, then we always want to mark the UID
                        // as removed and remove it from the UIDs list.
                        if (!processInTimestampOrder || !uids.contains(uid)) {
                            uidsToRemove.add(uid);
                            uids.remove(uid);
                        }
                    }
                    
                    quarantinedIds.addAll(v.getQUARANTINEUIDList());
                    
                    /*
                     * This is added for backwards compatibility. The removal list was added to ensure that removals are propagated across compactions. In the
                     * case where compactions did not occur, and the indices are converted into the newer protobuff, we must use the UID list to maintain
                     * removals for deltas less than 0
                     */
                    for (String uid : v.getUIDList()) {
                        // add to uidsToRemove, and decrement count if the uid is in UIDS
                        uidsToRemove.add(uid);
                        uids.remove(uid);
                    }
                }
                
            } catch (InvalidProtocolBufferException e) {
                if (key.isDeleted()) {
                    log.warn("Value passed to aggregator for a delete key was not of type Uid.List");
                } else {
                    log.error("Value passed to aggregator was not of type Uid.List", e);
                }
            }
        }
        return aggregate();
    }
    
    public void reset() {
        log.debug("Resetting GlobalIndexUidAggregator");
        count = 0;
        seenIgnore = false;
        builder = Uid.List.newBuilder();
        uids.clear();
        uidsToRemove.clear();
        releasedUids.clear();
        quarantinedIds.clear();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see datawave.ingest.table.aggregator.PropogatingAggregator#propogateKey()
     */
    @Override
    public boolean propogateKey() {
        
        /*
         * Changed logic so that if seenIgnore is true and count > MAX, we keep propagate the key
         */
        if ((seenIgnore && count > maxUids) || !quarantinedIds.isEmpty())
            return true;
        
        HashSet<String> uidsCopy = new HashSet<>(uids);
        uidsCopy.removeAll(uidsToRemove);
        
        if (log.isDebugEnabled()) {
            log.debug("{} {} {} {} removing {}", count, uids.size(), uidsToRemove.size(), uidsCopy.size(), (count == 0 && uidsCopy.isEmpty()));
        }
        
        // if <= 0 and uids is empty, we can safely remove
        return count > 0 || !uidsCopy.isEmpty();
    }
    
    @Override
    public IteratorOptions describeOptions() {
        IteratorOptions io = super.describeOptions();
        io.addNamedOption(MAX_UIDS_OPT, "The maximum number of UIDs to keep in the list. Default is " + MAX + ".");
        io.addNamedOption(PROCESS_IN_TIMESTAMP_ORDER, "Process in timestamp order (insert UID after remove UID will keep the UID). Default is false.");
        return io;
    }
    
    @Override
    public boolean validateOptions(Map<String,String> options) {
        boolean valid = super.validateOptions(options);
        if (valid) {
            if (options.containsKey(MAX_UIDS_OPT)) {
                maxUids = Integer.parseInt(options.get(MAX_UIDS_OPT));
                if (maxUids <= 0) {
                    throw new IllegalArgumentException("Max UIDs must be greater than 0.");
                }
            }
            if (options.containsKey(PROCESS_IN_TIMESTAMP_ORDER)) {
                processInTimestampOrder = Boolean.parseBoolean(options.get(PROCESS_IN_TIMESTAMP_ORDER));
            }
        }
        return valid;
    }
    
    @Override
    public SortedKeyValueIterator<Key,Value> deepCopy(IteratorEnvironment env) {
        GlobalIndexUidAggregator copy = (GlobalIndexUidAggregator) super.deepCopy(env);
        copy.maxUids = maxUids;
        // Not copying other fields that are all cleared in the reset() method.
        return copy;
    }
    
    @Override
    public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
        super.init(source, options, env);
        if (options.containsKey(MAX_UIDS_OPT)) {
            maxUids = Integer.parseInt(options.get(MAX_UIDS_OPT));
        }
    }
    
    public static void setMaxUidsOpt(IteratorSetting is, int maxUids) {
        is.addOption(MAX_UIDS_OPT, Integer.toString(maxUids));
    }
    
    public static void setProcessInTimestampOrderOpt(IteratorSetting is, boolean processInTimestampOrder) {
        is.addOption(PROCESS_IN_TIMESTAMP_ORDER, Boolean.toString(processInTimestampOrder));
    }
}
