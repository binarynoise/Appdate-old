package de.binarynoise.appdate.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.binarynoise.appdate.R;
import de.binarynoise.appdate.app.App;
import de.binarynoise.appdate.app.AppList;

import static de.binarynoise.appdate.SFC.sfcm;

/**
 * A fragment representing a list of {@link App}s.
 * <p>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class AppOverviewFragment extends Fragment {
	@Nullable private OnListFragmentInteractionListener mListener;
	private           RecyclerView                      recyclerView;
	
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnListFragmentInteractionListener)
			mListener = (OnListFragmentInteractionListener) context;
		else
			throw new RuntimeException(context + " must implement OnListFragmentInteractionListener");
		AppList.sortListAndUpdate();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sfcm.sfc.appOverviewFragment = this;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_app_overview, container, false);
		
		if (view instanceof RecyclerView) {
			Context context = view.getContext();
			recyclerView = (RecyclerView) view;
			recyclerView.setLayoutManager(new LinearLayoutManager(context));
			if (mListener != null)
				recyclerView.setAdapter(new AppOverviewRecyclerView(mListener));
			else
				throw new NullPointerException("mListener must not be null");
		}
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		requireContext();
		requireActivity();
		sfcm.sfc.appOverviewFragment = this;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (sfcm.sfc.appOverviewFragment == this)
			sfcm.sfc.appOverviewFragment = null;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}
	
	public void updateListView() {
		if (recyclerView != null && recyclerView.getAdapter() != null && sfcm.sfc.mainActivity != null)
			sfcm.sfc.mainActivity.runOnUiThread(() -> recyclerView.getAdapter().notifyDataSetChanged());
	}
	
	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 */
	@FunctionalInterface
	public interface OnListFragmentInteractionListener {
		void onListFragmentInteraction(App mItem);
	}
	
	private static class AppOverviewListLayout extends RecyclerView.ViewHolder {
		private final View     mView;
		private final TextView mNameView;
		private final TextView mVersionView;
		private       App      mItem;
		
		AppOverviewListLayout(View view) {
			super(view);
			mView = view;
			mNameView = view.findViewById(R.id.appOverview_name);
			mVersionView = view.findViewById(R.id.appOverview_version);
		}
		
		View getmView() {
			return mView;
		}
		
		TextView getmNameView() {
			return mNameView;
		}
		
		TextView getmVersionView() {
			return mVersionView;
		}
		
		App getmItem() {
			return mItem;
		}
		
		void setmItem(App mItem) {
			this.mItem = mItem;
		}
	}
	
	/**
	 * {@link RecyclerView.Adapter} that can display a {@link App} and makes a call to the
	 * specified {@link OnListFragmentInteractionListener}.
	 */
	private static class AppOverviewRecyclerView extends RecyclerView.Adapter<AppOverviewListLayout> {
		private final OnListFragmentInteractionListener mListener;
		
		AppOverviewRecyclerView(OnListFragmentInteractionListener mListener) {this.mListener = mListener;}
		
		@NonNull
		@Override
		public AppOverviewListLayout onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_overview_list_item, parent, false);
			return new AppOverviewListLayout(view);
		}
		
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		@Override
		public void onBindViewHolder(AppOverviewListLayout appOverviewListLayout, int position) {
			App app = AppList.get(position);
			appOverviewListLayout.setmItem(app);
			appOverviewListLayout.getmNameView().setText(app.installedName);
			appOverviewListLayout.getmVersionView().setText(app.getInstalledVersionString());
			
			appOverviewListLayout.getmView().setOnClickListener(v -> {
				// Notify the active callbacks interface (the activity, if the
				// fragment is attached to one) that an item has been selected.
				if (mListener != null)
					mListener.onListFragmentInteraction(appOverviewListLayout.getmItem());
			});
		}
		
		@Override
		public int getItemCount() {
			return AppList.size();
		}
	}
}
