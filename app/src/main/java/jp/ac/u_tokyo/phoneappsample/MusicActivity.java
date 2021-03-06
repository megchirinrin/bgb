package jp.ac.u_tokyo.phoneappsample;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicActivity extends AppCompatActivity {
    private String TAG = getClass().getSimpleName();
    public static final List<Music> idList = Arrays.asList(
      new Music(R.raw.bonodori1, "北海盆唄（北海道）", "1"),
      new Music(R.raw.bonodori5,"なにゃとらや（青森県）", "2"),
      new Music(R.raw.bonodori2, "東京音頭（東京都）","3"),
      new Music(R.raw.bonodori3, "河内音頭（大阪府）","4"),
      new Music(R.raw.bonodori4,"よさこい節（高知県）","5")
    );

    public static Music findMusicById(String id) {
        for (int i = 0; i < idList.size(); i++) {
            Music music = idList.get(i);
            if (music.id.equals(id)) {
                return music;
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        ListView listView = (ListView) findViewById(R.id.listview_music);
        MyAdapter adapter = new MyAdapter(this, 0, idList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Music music = idList.get(i);
                Intent data = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString("music_id", music.id);
                Log.d(TAG, "Music Click id:" + music.id);
                data.putExtras(bundle);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    public static class Music {
        public int resourceId;
        public String name;
        public String id;

        Music(int resourceId, String name, String id) {
            this.resourceId = resourceId;
            this.name = name;
            this.id = id;
        }
    }

    private class MyAdapter extends ArrayAdapter<Music> {
        private LayoutInflater inflater;

        MyAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Music> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item, null, false);
            TextView textView = (TextView) view.findViewById(R.id.item_textview);
            String name = getItem(position).name;
            textView.setText(name);
            return view;
        }
    }
}
