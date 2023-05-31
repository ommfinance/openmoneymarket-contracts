package finance.omm.score.staking.db;

import score.Address;
import score.Context;
import score.VarDB;

import java.math.BigInteger;

import static finance.omm.score.staking.db.LinkedListDB.DEFAULT_NODE_ID;

public class NodeDB {
    private static final String NAME = "_NODEDB";

    private final VarDB<BigInteger> value;
    private final VarDB<Address> key;
    private final VarDB<BigInteger> blockHeight;
    private final VarDB<Address> senderAddress;
    private final VarDB<BigInteger> next;
    private final VarDB<BigInteger> prev;

    public NodeDB(String key) {
        String name = key + NAME;
        this.value = Context.newVarDB(name + "_value", BigInteger.class);
        this.key = Context.newVarDB(name + "_key", Address.class);
        this.blockHeight = Context.newVarDB(name + "_block_height", BigInteger.class);
        this.senderAddress = Context.newVarDB(name + "_address", Address.class);
        this.next = Context.newVarDB(name + "_next", BigInteger.class);
        this.prev = Context.newVarDB(name + "_prev", BigInteger.class);
    }

    public void delete() {
        value.set(null);
        key.set(null);
        blockHeight.set(null);
        senderAddress.set(null);
        prev.set(null);
        next.set(null);
    }

    public boolean exists() {
        return value.get() != null;
    }

    public BigInteger getValue() {
        return value.getOrDefault(BigInteger.ZERO);
    }

    public Address getKey() {
        return key.get();
    }

    public BigInteger getBlockHeight() {
        return blockHeight.getOrDefault(BigInteger.ZERO);
    }

    public Address getSenderAddress() {
        return senderAddress.get();
    }

    public void setValues(Address key, BigInteger value, BigInteger blockHeight, Address senderAddress) {
        this.value.set(value);
        this.key.set(key);
        this.blockHeight.set(blockHeight);
        this.senderAddress.set(senderAddress);
    }

    public BigInteger getNext() {
        return next.getOrDefault(DEFAULT_NODE_ID);
    }

    public void setNext(BigInteger nextId) {
        next.set(nextId);
    }

    public BigInteger getPrev() {
        return prev.getOrDefault(DEFAULT_NODE_ID);
    }

    public void setPrev(BigInteger prev_id) {
        prev.set(prev_id);
    }

}
