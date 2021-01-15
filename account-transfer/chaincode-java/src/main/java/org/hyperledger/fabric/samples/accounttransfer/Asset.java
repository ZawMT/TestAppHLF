package org.hyperledger.fabric.samples.accounttransfer;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

public final class Asset {
    @Property()
    private final Integer id;

    @Property()
    private final Integer balance;

    @Property()
    private final String owner;

    public Integer getAssetID() {
        return id;
    }

    public Integer getBalance() {
        return balance;
    }

    public String getOwner() {
        return owner;
    }

    public Asset(@JsonProperty("id") final Integer id, @JsonProperty("balance") final Integer balance, @JsonProperty("owner") final String owner) {
        this.id = id;
        this.balance = balance;
        this.owner = owner;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Asset other = (Asset) obj;
        return this.id == other.getAssetID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssetID(), getBalance(), getOwner());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [assetID=" + Integer.toString(id) + ", balance="
                + Integer.toString(balance) +  ", owner=" + owner + "]";
    }
}
