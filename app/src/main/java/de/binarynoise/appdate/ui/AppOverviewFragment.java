package de.binarynoise.appdate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import de.binarynoise.appdate.R;
import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;

import static de.binarynoise.appdate.SFC.sfcm;

public class AppOverviewFragment extends Fragment {
	private static boolean            isRefreshing = false;
	private        SwipeRefreshLayout swipeRefreshLayout;
	private        RecyclerView       list;
	
	public static void setRefreshing(boolean refreshing) {
		isRefreshing = refreshing;
		if (sfcm.sfc.appOverviewFragment != null)
			runOnUiThread(() -> sfcm.sfc.appOverviewFragment.swipeRefreshLayout.setRefreshing(refreshing));
	}
	
	public static void runOnUiThread(Runnable r) {
		if (sfcm.sfc.mainActivity != null)
			sfcm.sfc.mainActivity.runOnUiThread(r);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sfcm.sfc.appOverviewFragment = this;
	}
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_app_overview, container, false);
		
		swipeRefreshLayout = view.findViewById(R.id.appOverview_refreshLayout);
		swipeRefreshLayout.setOnRefreshListener(() -> new Thread(AppList::checkForUpdates).start());
		
		list = view.findViewById(R.id.appOverview_list);
		list.setAdapter(new AppOverviewListItemAdapter());
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		requireContext();
		requireActivity();
		sfcm.sfc.appOverviewFragment = this;
		updateListView();
		setRefreshing(isRefreshing);
		for (int i = 0; i < AppList.size() - 1; i++)
			AppList.get(i).updateListItem();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		for (int i = 0; i < AppList.size() - 1; i++)
			AppList.get(i).updateListItem();
		updateListView();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (sfcm.sfc.appOverviewFragment == this)
			sfcm.sfc.appOverviewFragment = null;
	}
	
	public void updateListView() {
		if (list != null && list.getAdapter() != null) {
			FragmentActivity activity = getActivity();
			if (activity != null)
				activity.runOnUiThread(() -> list.getAdapter().notifyDataSetChanged());
		}
	}
	
	public static class AppOverviewListItem extends RecyclerView.ViewHolder {
		public final View             view;
		public final TextView         name;
		public final TextView         versionInstalled;
		public final TextView         versionUpdate;
		public final ProgressBar      placeholder;
		public final ImageView        icon;
		public final ConstraintLayout container;
		
		AppOverviewListItem(View view) {
			super(view);
			this.view = view;
			name = view.findViewById(R.id.appOverview_list_name);
			versionInstalled = view.findViewById(R.id.appOverview_list_version_installed);
			versionUpdate = view.findViewById(R.id.appOverview_list_version_update);
			placeholder = view.findViewById(R.id.appOverview_list_icon_placeholder);
			icon = view.findViewById(R.id.appOverview_list_icon);
			container = view.findViewById(R.id.appOverview_list_container);
		}
	}
	
	static class AppOverviewListItemAdapter extends RecyclerView.Adapter<AppOverviewListItem> {
		@NonNull
		@Override
		public AppOverviewListItem onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_overview_list_item, parent, false);
			return new AppOverviewListItem(view);
		}
		
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void onBindViewHolder(@NonNull AppOverviewListItem listItem, int position) {
			App app = AppList.get(position);
			app.registerListItem(listItem);
		}
		
		@Override
		public int getItemCount() {
			return AppList.size();
		}
	}
}
