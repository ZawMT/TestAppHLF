package org.hyperledger.fabric.samples.accounttransfer;

import java.util.ArrayList;
import java.util.List;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(
        name = "scon2",
        info = @Info(
                title = "Asset Transfer",
                description = "The hyperlegendary asset transfer",
                version = "0.0.1",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "mg.zawmintun@gmail.com",
                        name = "Zaw Min Tun",
                        url = "http://corner-z.com")))
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS,
        ASSET_NOT_SUFFICIENT_FUND
    }

    /**
     * Creates some initial assets on the ledger.
     *
     * @param ctx the transaction context
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        CreateAsset(ctx, 1, 150, "Alex");
        CreateAsset(ctx, 2, 500, "Bryan");
        CreateAsset(ctx, 3, 250, "Catherine");
    }

    /**
     * Creates a new asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the new asset
     * @param color the color of the new asset
     * @param size the size for the new asset
     * @param owner the owner of the new asset
     * @param appraisedValue the appraisedValue of the new asset
     * @return the created asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx, final Integer assetID, final Integer balance, final String owner) {
        ChaincodeStub stub = ctx.getStub();

        if (AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s already exists", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        Asset asset = new Asset(assetID, balance, owner);
        String assetJSON = genson.serialize(asset);
        stub.putStringState(Integer.toString(assetID), assetJSON);
        return asset;
    }

    /**
     * Retrieves an asset with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return the asset found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetID);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);
        return asset;
    }

    /**
     * Updates the properties of an asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being updated
     * @param color the color of the asset being updated
     * @param size the size of the asset being updated
     * @param owner the owner of the asset being updated
     * @param appraisedValue the appraisedValue of the asset being updated
     * @return the transferred asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset UpdateAsset(final Context ctx, final Integer assetID, final Integer balance, final String owner) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetID)) {
            String errorMessage = String.format("Asset %s does not exist", assetID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset newAsset = new Asset(assetID, balance, owner);
        String newAssetJSON = genson.serialize(newAsset);
        stub.putStringState(Integer.toString(assetID), newAssetJSON);
        return newAsset;
    }

    /**
     * Checks the existence of the asset on the ledger
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset
     * @return boolean indicating the existence of the asset
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final Integer assetID) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(Integer.toString(assetID));

        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Changes the owner of a asset on the ledger.
     *
     * @param ctx the transaction context
     * @param assetID the ID of the asset being transferred
     * @param newOwner the new owner
     * @return the updated asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset TransferAsset(final Context ctx, final Integer assetIDFrom, final Integer assetIDTo, final Integer amount) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSONFrom = stub.getStringState(Integer.toString(assetIDFrom));
        if (assetJSONFrom == null || assetJSONFrom.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist",  assetJSONFrom);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        String assetJSONTo = stub.getStringState(Integer.toString(assetIDTo));
        if (assetJSONTo == null || assetJSONTo.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetJSONTo);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset assetFrom = genson.deserialize(Integer.toString(assetIDFrom), Asset.class);
        Asset assetTo = genson.deserialize(Integer.toString(assetIDTo), Asset.class);

        if (assetFrom.getBalance() < amount) {
            String errorMessage = String.format("Asset %s has no sufficient fund to do this transaction",  assetJSONFrom);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_SUFFICIENT_FUND.toString());
        }

        Asset newAssetFrom = new Asset(assetFrom.getAssetID(), assetFrom.getBalance() - amount, assetFrom.getOwner());
        String newAssetJSONFrom = genson.serialize(newAssetFrom);

        Asset newAssetTo = new Asset(assetTo.getAssetID(), assetTo.getBalance() - amount, assetTo.getOwner());
        String newAssetJSONTo = genson.serialize(newAssetTo);

        stub.putStringState(Integer.toString(assetIDFrom), newAssetJSONFrom);
        stub.putStringState(Integer.toString(assetIDTo), newAssetJSONTo);

        return newAssetFrom;
    }

    /**
     * Retrieves all assets from the ledger.
     *
     * @param ctx the transaction context
     * @return array of assets found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<Asset> queryResults = new ArrayList<Asset>();

        // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
        // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
        // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
        // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            queryResults.add(asset);
            System.out.println(asset.toString());
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
}
