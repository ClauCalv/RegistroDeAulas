package br.ufabc.gravador.controls.helpers;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConnectionHelper {

    public ConnectionHelper ( Context context ) {
        this.context = context;
        mConnectionsClient = Nearby.getConnectionsClient(context);
        endpointID = context.getPackageName();
    }

    private final Context context;
    private ConnectionsClient mConnectionsClient;
    private static final Strategy strategy = Strategy.P2P_STAR;
    private final String endpointID;
    private ConnectionCallback listener = new EmptyConnectionListener();

    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();
    private final Map<String, Endpoint> mEstablishedConnections = new HashMap<>();
    private final Map<Long, Payload> incomingPayloads = new HashMap<>();

    private boolean mIsConnecting = false;
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback = new ConnectionLifecycleCallback() {

        @Override
        public void onConnectionInitiated ( String endpointId, ConnectionInfo connectionInfo ) {
            Log.d("CONN", String.format(
                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                    endpointId, connectionInfo.getEndpointName()
            ));

            Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
            mPendingConnections.put(endpointId, endpoint);
            ConnectionHelper.this.onConnectionInitiated(endpoint, connectionInfo);
        }

        @Override
        public void onConnectionResult ( String endpointId, ConnectionResolution result ) {
            Log.d("CONN", String.format(
                    "onConnectionResponse(endpointId=%s, result=%s)",
                    endpointId, result
            ));

            mIsConnecting = false;

            if ( !result.getStatus().isSuccess() ) {
                Log.d("CONN", String.format(
                        "Connection failed. Received status %s.",
                        statusToString(result.getStatus())
                ));
                onConnectionFailed(mPendingConnections.remove(endpointId), null);
                return;
            }
            connectedToEndpoint(mPendingConnections.remove(endpointId));
        }

        @Override
        public void onDisconnected ( String endpointId ) {
            if ( !mEstablishedConnections.containsKey(endpointId) ) {
                Log.w("CONN", "Unexpected disconnection from endpoint " + endpointId);
                return;
            }
            disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
        }
    };

    private final PayloadCallback mPayloadCallback = new PayloadCallback() {

        @Override
        public void onPayloadReceived ( String endpointId, Payload payload ) {
            Log.d("CONN", String.format(
                    "onPayloadReceived(endpointId=%s, payload=%s)",
                    endpointId, payload
            ));
            onReceive(mEstablishedConnections.get(endpointId), payload);
        }

        @Override
        public void onPayloadTransferUpdate ( String endpointId, PayloadTransferUpdate update ) {
            Log.d("CONN", String.format(
                    "onPayloadTransferUpdate(endpointId=%s, update=%s)",
                    endpointId, update
            ));
        }
    };

    private static String statusToString ( Status status ) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    public void startAdvertising ( String nameToDisplay ) {
        mIsAdvertising = true;
        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(strategy); //TODO

        mConnectionsClient
                .startAdvertising(
                        nameToDisplay, endpointID, mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(this::onAdvertisingStarted)
                .addOnFailureListener(this::onAdvertisingFailed);
    }

    public void stopAdvertising () {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    public boolean isAdvertising () {
        return mIsAdvertising;
    }

    public void acceptConnection ( final Endpoint endpoint ) {
        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(( e ) -> Log.w("CONN", "acceptConnection() failed.", e));
    }

    public void rejectConnection ( Endpoint endpoint ) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(( e ) -> Log.w("CONN", "rejectConnection() failed.", e));
    }

    public void startDiscovering () {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();

        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(strategy);

        mConnectionsClient
                .startDiscovery(
                        endpointID,
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound ( String endpointId, DiscoveredEndpointInfo info ) {
                                Log.d("CONN", String.format(
                                        "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                        endpointId, info.getServiceId(), info.getEndpointName()));

                                if ( endpointID.equals(info.getServiceId()) ) {
                                    Endpoint endpoint = new Endpoint(endpointId,
                                            info.getEndpointName());
                                    mDiscoveredEndpoints.put(endpointId, endpoint);
                                    onEndpointDiscovered(endpoint);
                                }
                            }

                            @Override
                            public void onEndpointLost ( String endpointId ) {
                                Log.d("CONN",
                                        String.format("onEndpointLost(endpointId=%s)", endpointId));
                            }
                        },
                        discoveryOptions.build())

                .addOnSuccessListener(this::onDiscoveryStarted)
                .addOnFailureListener(this::onDiscoveryFailed);
    }

    public void stopDiscovering () {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    public boolean isDiscovering () {
        return mIsDiscovering;
    }

    public void disconnect ( Endpoint endpoint ) {
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    public void disconnectFromAllEndpoints () {
        for ( Endpoint endpoint : mEstablishedConnections.values() )
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.clear();
    }

    public void stopAllEndpoints () {
        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        mIsConnecting = false;
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    public void connectToEndpoint ( final Endpoint endpoint, String nameToDisplay ) {
        Log.v("CONN", "Sending a connection request to endpoint " + endpoint);
        mIsConnecting = true;

        mConnectionsClient
                .requestConnection(nameToDisplay, endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(( e ) -> onConnectionFailed(endpoint, e));
    }

    public final boolean isConnecting () {
        return mIsConnecting;
    }

    private void connectedToEndpoint ( Endpoint endpoint ) {
        Log.d("CONN", String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint ( Endpoint endpoint ) {
        Log.d("CONN", String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    public Set<Endpoint> getDiscoveredEndpoints () {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    public Set<Endpoint> getConnectedEndpoints () {
        return new HashSet<>(mEstablishedConnections.values());
    }

    public void send ( Payload payload ) {
        send(payload, mEstablishedConnections.keySet());
    }

    private void send ( Payload payload, Set<String> endpoints ) {
        mConnectionsClient
                .sendPayload(new ArrayList<>(endpoints), payload)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure ( Exception e ) {
                        Log.w("COMM", "sendPayload() failed.", e);
                    }
                });
    }

    public void setConnectionListener(ConnectionCallback listener) {
        this.listener = listener;
    }

    protected void onAdvertisingStarted ( Void unused ) {
        listener.onAdvertisingStarted();
    }

    protected void onAdvertisingFailed ( Exception e ) {
        Log.w("CONN", "advertising failed.", e);
        listener.onAdvertisingFailed();
    }

    protected void onConnectionInitiated ( Endpoint endpoint, ConnectionInfo connectionInfo ) {
        listener.onConnectionInitiated(endpoint, connectionInfo);
    }

    protected void onDiscoveryStarted ( Void unused ) {
        listener.onDiscoveryStarted();
    }

    protected void onDiscoveryFailed ( Exception e ) {
        Log.w("CONN", "discovery failed.", e);
        listener.onDiscoveryFailed();
    }

    protected void onEndpointDiscovered ( Endpoint endpoint ) {
        listener.onEndpointDiscovered(endpoint);
    }

    protected void onConnectionFailed ( Endpoint endpoint, Exception e ) {
        Log.w("CONN", "requestConnection() failed.", e);
        mIsConnecting = false;
        listener.onConnectionFailed(endpoint);
    }

    protected void onEndpointConnected ( Endpoint endpoint ) {
        listener.onEndpointConnected(endpoint);
    }

    protected void onEndpointDisconnected ( Endpoint endpoint ) {
        listener.onEndpointDisconnected(endpoint);
    }

    protected void onReceive ( Endpoint endpoint, Payload payload ) {

    }

    public interface ConnectionCallback {
        void onAdvertisingStarted ();

        void onAdvertisingFailed ();

        void onConnectionInitiated ( Endpoint endpoint, ConnectionInfo connectionInfo );

        void onDiscoveryStarted ();

        void onDiscoveryFailed ();

        void onEndpointDiscovered ( Endpoint endpoint );

        void onConnectionFailed ( Endpoint endpoint );

        void onEndpointConnected ( Endpoint endpoint );

        void onEndpointDisconnected ( Endpoint endpoint );
    }

    public static class EmptyConnectionListener implements ConnectionCallback {
        public void onAdvertisingStarted () {}

        public void onAdvertisingFailed () {}

        public void onConnectionInitiated ( Endpoint endpoint, ConnectionInfo connectionInfo ) {}

        public void onDiscoveryStarted () {}

        public void onDiscoveryFailed () {}

        public void onEndpointDiscovered ( Endpoint endpoint ) {}

        public void onConnectionFailed ( Endpoint endpoint ) {}

        public void onEndpointConnected ( Endpoint endpoint ) {}

        public void onEndpointDisconnected ( Endpoint endpoint ) {}
    }

    public static class Endpoint {

        private final String id;
        private final String name;

        private Endpoint ( String id, String name ) {
            this.id = id;
            this.name = name;
        }

        public String getId () { return id; }

        public String getName () { return name; }

        @Override
        public boolean equals ( Object obj ) {
            if ( obj instanceof Endpoint ) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode () {
            return id.hashCode();
        }

        @Override
        public String toString () {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }
}