package jp.ac.u_tokyo.phoneappsample;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;

public class MainActivity extends AppCompatActivity {
    private static final int RECORD_AUDIO_REQUEST_ID = 1;
    private static final int MUSIC_REQUEST_ID = 2;
    private String TAG = getClass().getSimpleName();
    private Peer peer;
    private MediaConnection connection;
    private String currentId;
    private TextView idTextView;
    private ListView listView;
    private MyAdapter adapter;
    private List<String> idList = new ArrayList<String>();
    private MediaPlayer player;

    private MusicActivity.Music music = MusicActivity.idList.get(0);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_IN_COMMUNICATION);

        checkAudioPermission();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initPeer();

        idTextView = (TextView) findViewById(R.id.id_textview);
        listView = (ListView) findViewById(R.id.listview);

        adapter = new MyAdapter(this, 0, idList);
        listView.setAdapter(adapter);

        Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshPeerList();
            }
        });

        Button closeBtn = (Button) findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConnection();
            }
        });

        Button musicBtn = (Button) findViewById(R.id.music_btn);
        musicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMusicActivity();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedPeerId = idList.get(i);
                if (selectedPeerId == null) {
                    Log.d(TAG, "Selected PeerId == null");
                    return;
                }
                Log.d(TAG, "SelectedPeerId: " + selectedPeerId);
                call(selectedPeerId);
            }
        });

        showCurrentPeerId();

        refreshPeerList();

        peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "CALL Event is Received");
                if (o instanceof MediaConnection) {
                    Log.d(TAG, "CALL Event is Received and MediaConnection");
                    final MediaConnection connection = (MediaConnection) o;

                    if (MainActivity.this.connection != null) {
                        Log.d(TAG, "connection is already created");
                        connection.close();
                        return;
                    }

                    Log.d(TAG, "CALL Dialog is showing");
                    // TODO: show dialog

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("着信アリ")
                                    .setMessage(connection.connectionId)
                                    .setPositiveButton("出たい？", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MediaStream stream = MainActivity.this.getMediaStream();

                                            setMusic(connection.metadata);
                                            connection.answer(stream);

                                            setConnectionCallback(connection);
                                            startMusic();

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    idTextView.setText("受信中");
                                                }
                                            });

                                            MainActivity.this.connection = connection;
                                            Log.d(TAG, "CALL Event is Received and Set");
                                        }
                                    })
                                    .setNegativeButton("出たくない？", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            connection.close();
                                            Log.d(TAG, "CALL Event is Received and closed");
                                        }
                                    })
                                    .show();
                            Log.d(TAG, "CALL Dialog is shown");
                        }
                    });

                    return;
                }
                Log.d(TAG, "CALL Event is Received and NOT MediaConnection");
            }
        });

        peer.on(Peer.PeerEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "Peer Error Occured");
                Log.d(TAG, o.getClass().getName());
                if (o instanceof PeerError) {
                    PeerError error = (PeerError) o;
                    Log.d(TAG, error.message);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connection != null) {
            closeConnection();
        }
        if (peer != null && !peer.isDestroyed) {
            peer.destroy();
            peer = null;
        }
        stopMusic();
    }

    private void initPeer(){
        if (peer != null) {
            return;
        }
        PeerOption options = new PeerOption();
        options.key = BuildConfig.SKYWAY_API_KEY;
        options.domain = BuildConfig.SKYWAY_HOST;
        peer = new Peer(this, options);
        Navigator.initialize(peer);
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Manifest.permission.RECORD_AUDIO is not GRANTED");
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Log.d(TAG, "shouldShowRequestPermissionRationale = false");
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                Log.d(TAG, "request Permission RECORD_AUDIO");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        RECORD_AUDIO_REQUEST_ID);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RECORD_AUDIO_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "request Permission RECORD_AUDIO GRANTED!");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d(TAG, "request Permission RECORD_AUDIO DENIED!");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void showCurrentPeerId() {
        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (o instanceof String) {
                    currentId = (String) o;
                    Log.d(TAG, "currentId: " + currentId);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            idTextView.setText("ID: " + currentId);
                        }
                    });
                }
            }
        });
    }

    private void refreshPeerList() {
        Log.d(TAG, "Refreshing");
        peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (o instanceof JSONArray) {
                    JSONArray array = (JSONArray) o;
                    idList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        try {
                            String id = array.getString(i);
                            idList.add(id);
                            Log.d(TAG, "Fetched PeerId: " + id);
                        } catch (JSONException e) {
                            Log.e(TAG, "Parse ListAllPeer", e);
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    private void call(String peerId) {
        Log.d(TAG, "Calling to id:" + peerId);
        if (peer == null) {
            Log.i(TAG, "Call but peer is null");
            return;
        }

        if (peer.isDestroyed || peer.isDisconnected) {
            Log.i(TAG, "Call but peer is not active");
            return;
        }

        if (connection != null) {
            Log.d(TAG, "Call but connection is already created");
            return;
        }

        MediaStream stream = getMediaStream();

        if (stream == null) {
            Log.d(TAG, "Call but media stream is null");
            return;
        }
        CallOption option = new CallOption();
        option.metadata = this.music.id; // TODO: metadata
        MediaConnection connection = peer.call(peerId, stream, option);

        if (connection == null) {
            Log.d(TAG, "Call but MediaConnection is null");
            return;
        }

        setConnectionCallback(connection);
        startMusic();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                idTextView.setText("発信中");
            }
        });

        this.connection = connection;
        Log.d(TAG, "connection started!");
    }

    private MediaStream getMediaStream() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.videoFlag = false;
        constraints.audioFlag = true;
        return Navigator.getUserMedia(constraints);
    }

    private void setConnectionCallback(MediaConnection connection) {
        connection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "MediaConnection Close Event is Received");
                closeConnection();
            }
        });
        connection.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "MediaConnection Error Occured");
                Log.d(TAG, o.getClass().getName());
                if (o instanceof PeerError) {
                    PeerError error = (PeerError) o;
                    Log.d(TAG, error.message);
                }
            }
        });
        connection.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "MediaConnection STREAM Occured");
                Log.d(TAG, o.getClass().getName());
                if (o instanceof MediaStream) {
                    MediaStream stream = (MediaStream) o;
                    if (MainActivity.this.connection != null) {
                        MainActivity.this.connection.answer(stream);
                    }
                }
            }
        });
    }

    private void removeConnectionCallback(MediaConnection connection) {
        connection.on(MediaConnection.MediaEventEnum.CLOSE, null);
        connection.on(MediaConnection.MediaEventEnum.ERROR, null);
        connection.on(MediaConnection.MediaEventEnum.STREAM, null);
    }

    private void closeConnection() {
        if (connection != null) {
            removeConnectionCallback(connection);
            connection.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    idTextView.setText("通話終了");
                }
            });
            MainActivity.this.connection = null;
            Log.d(TAG, "Connection is Closed");

            stopMusic();
        }
    }

    private void startMusic() {
        if (player != null) {
            Log.d(TAG, "MediaPlayer is already created");
            return;
        }
        player = MediaPlayer.create(getApplicationContext(), music.resourceId);
        if (player == null) {
            Log.e(TAG, "MediaPlayer is not created");
            return;
        }
        player.setLooping(true);
        player.start();
        Log.d(TAG, "MediaPlayer started");
    }

    private void stopMusic() {
        if (player == null) {
            return;
        }
        player.stop();
        player.reset();
        player.release();
        player = null;
    }

    private void showMusicActivity(){
        Intent intent = new Intent(MainActivity.this, MusicActivity.class);
        startActivityForResult(intent, MUSIC_REQUEST_ID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case MUSIC_REQUEST_ID:
                Log.d(TAG, "onActivityResult: MUSIC_REQUEST_ID");
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "onActivityResult: RESULT_OK");
                    Bundle bundle = data.getExtras();
                    Log.d(TAG, "onActivityResult: accept id:" + bundle.getString("music_id"));
                    setMusic(bundle.getString("music_id"));
                }
        }
    }

    private void setMusic(String id) {
        MusicActivity.Music music = MusicActivity.findMusicById(id);
        if (music != null) {
            Log.d(TAG, "onActivityResult: music_id" + music.id);
            this.music = music;
        }
    }

    private class MyAdapter extends ArrayAdapter<String> {
        private LayoutInflater inflater;

        MyAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item, null, false);
            TextView textView = (TextView) view.findViewById(R.id.item_textview);
            String name = getItem(position);
            textView.setText(name);
            return view;
        }
    }
}

