package org.nv95.openmanga;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.nv95.openmanga.providers.LocalMangaProvider;
import org.nv95.openmanga.providers.MangaInfo;
import org.nv95.openmanga.providers.MangaList;
import org.nv95.openmanga.providers.MangaProvider;
import org.nv95.openmanga.providers.MangaProviderManager;

import java.io.IOException;

/**
 * Created by nv95 on 30.09.15.
 *
 */
public class MangaListFragment extends Fragment implements AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {
    private AbsListView absListView;
    private boolean grid = false;
    private MangaListAdapter adapter;
    private MangaProvider provider;
    private MangaList list;
    private ProgressBar progressBar;
    private EndlessScroller endlessScroller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mangalist,
                container, false);
        endlessScroller = new EndlessScroller(view) {
            @Override
            public boolean onNextPage(int page) {
                if (provider.hasFeature(MangaProviderManager.FUTURE_MULTIPAGE)) {
                    new ListLoadTask().execute(page);
                    return true;
                } else {
                    return false;
                }
            }
        };
        grid = PreferenceManager.getDefaultSharedPreferences(inflater.getContext()).getBoolean("grid",false);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        GridView gridView = (GridView) view.findViewById(R.id.gridView);
        listView.setOnItemClickListener(this);
        gridView.setOnItemClickListener(this);
        listView.setOnScrollListener(endlessScroller);
        gridView.setOnScrollListener(endlessScroller);
        listView.setMultiChoiceModeListener(this);
        gridView.setMultiChoiceModeListener(this);
        listView.setVisibility(grid ? View.GONE : View.VISIBLE);
        gridView.setVisibility(grid ? View.VISIBLE : View.GONE);
        //listView.addFooterView(endlessScroller.getFooter());
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        absListView = grid ? gridView : listView;
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        provider = new LocalMangaProvider(getActivity());
        adapter = new MangaListAdapter(getActivity(),list = new MangaList(), grid);
        //((LocalMangaProvider)provider).test();

        absListView.setAdapter(adapter);
        //setGridLayout(grid);
        new ListLoadTask().execute();
    }

    public MangaProvider getProvider() {
        return provider;
    }

    public void setProvider(MangaProvider provider) {
        this.provider = provider;
        list.clear();
        adapter.notifyDataSetChanged();
        new ListLoadTask().execute();
    }

    public void setGridLayout(boolean useGrid) {
        if (useGrid != grid) {
            grid = useGrid;
            final int pos = absListView.getFirstVisiblePosition();
            absListView.setAdapter(null);
            ListView listView = (ListView) getView().findViewById(R.id.listView);
            GridView gridView = (GridView) getView().findViewById(R.id.gridView);
            listView.setVisibility(useGrid ? View.GONE : View.VISIBLE);
            gridView.setVisibility(useGrid ? View.VISIBLE : View.GONE);
            absListView = useGrid ? gridView : listView;
            absListView.setAdapter(adapter);
            adapter.setGrid(grid);
            absListView.setSelection(pos);
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("grid",grid).apply();
        }
    }

    public boolean isGridLayout() {
        return grid;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), MangaPreviewActivity.class);
        MangaInfo info = adapter.getMangaInfo(position);
        intent.putExtras(info.toBundle());
        startActivity(intent);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        String title = getString(R.string.selected) + " " + absListView.getCheckedItemCount();
        mode.setTitle(title);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.actionmode_mangas, menu);
        menu.findItem(R.id.action_remove).setVisible(provider.hasFeature(MangaProviderManager.FEAUTURE_REMOVE));
        return menu.hasVisibleItems();
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove:
                provider.remove(absListView.getCheckedItemIds());
                list.clear();
                progressBar.setVisibility(View.VISIBLE);
                new ListLoadTask().execute();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }


    private class ListLoadTask extends AsyncTask<Integer, Void, MangaList> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (list.size() == 0)
                progressBar.setVisibility(View.VISIBLE);
            /*if (listView.getFooterViewsCount() != 0)
                listView.removeFooterView(loadFooter);*/
        }

        @Override
        protected void onPostExecute(MangaList mangaInfos) {
            super.onPostExecute(mangaInfos);
            progressBar.setVisibility(View.GONE);
            if (mangaInfos == null) {
                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                endlessScroller.loadingFail();
            } else if (mangaInfos.size() == 0) {
                Toast.makeText(getActivity(), "No manga found", Toast.LENGTH_SHORT).show();
                endlessScroller.loadingFail();
            } else {
                list.addAll(mangaInfos);
                adapter.notifyDataSetChanged();
                endlessScroller.loadingDone();
            }
        }

        @Override
        protected MangaList doInBackground(Integer... params) {
            try {
                return provider.getList(params.length > 0 ? params[0] : 0);
            } catch (IOException e) {
                return null;
            }
        }
    }

    protected abstract class EndlessScroller implements AbsListView.OnScrollListener {
        private int page;
        private boolean loading;
        private TextView textView;
        private View footer;

        public EndlessScroller(View view) {
            footer = view.findViewById(R.id.frame_footer);
            textView = (TextView) footer.findViewById(R.id.textView_footer);
            progressBar = (ProgressBar) footer.findViewById(R.id.progressBar2);
            loading = true;
            footer.setVisibility(View.GONE);
        }

        public View getFooter() {
            return footer;
        }

        public int getPage() {
            return page;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (!loading && (totalItemCount - visibleItemCount) <= firstVisibleItem) {
                loading = onNextPage(page + 1);
                if (loading) {
                    footer.setVisibility(View.VISIBLE);
                    String s = getString(R.string.loading_page) + " " + (page + 1);
                    textView.setText(s);
                }
            }
        }

        public void loadingDone() {
            page++;
            loading = false;
            footer.setVisibility(View.GONE);
        }

        public void loadingFail() {
            footer.setVisibility(View.GONE);
        }

        public abstract boolean onNextPage(int page);
    }
}
