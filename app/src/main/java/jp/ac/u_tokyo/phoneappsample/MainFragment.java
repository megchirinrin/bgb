package jp.ac.u_tokyo.phoneappsample;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;



public class MainFragment extends Fragment {
    private String TAG = getClass().getSimpleName();

    private TextView idTextView;
    private ListView listView;
    private Button refreshBtn;
    private Button closeBtn;
    private MainActivity.MyAdapter adapter;
    public MainActivity mainActivity;

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(MainActivity main) {
        MainFragment fragment = new MainFragment();
        fragment.mainActivity = main;
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new MainActivity.MyAdapter(this.mainActivity, 0, this.mainActivity.idList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedPeerId = MainFragment.this.mainActivity.idList.get(i);
                if (selectedPeerId == null) {
                    Log.d(TAG, "Selected PeerId == null");
                    return;
                }
                Log.d(TAG, "SelectedPeerId: " + selectedPeerId);
                MainFragment.this.mainActivity.call(selectedPeerId);
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment.this.mainActivity.refreshPeerList();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainFragment.this.mainActivity.closeConnection();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        idTextView = (TextView) view.findViewById(R.id.id_textview);
        listView = (ListView) view.findViewById(R.id.listview);

        refreshBtn = (Button) view.findViewById(R.id.refresh_btn);

         closeBtn = (Button) view.findViewById(R.id.close_btn);


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void onSetId(String id) {
        idTextView.setText("ID: " + id);
    }

    public void onStartConnection() {
        idTextView.setText("発信中");
    }

    public void onCloseConnection() {
        idTextView.setText("通話終了");
    }

    public void onListChange() {
        adapter.notifyDataSetChanged();
    }
}
