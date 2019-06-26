package de.binarynoise.appdate;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.binarynoise.appdate.app.AppList;
import de.binarynoise.appdate.ui.AddAppFragment;
import de.binarynoise.appdate.ui.AppOverviewFragment;
import de.binarynoise.appdate.ui.MainActivity;
import de.binarynoise.appdate.ui.PreferencesFragment;

public class SFC {
	public static final SFC                   sfcm = new SFC();
	public final        StaticFieldsContainer sfc  = new StaticFieldsContainer();
	
	public static class StaticFieldsContainer {
		@Nullable public FloatingActionButton floatingActionButton;
		@Nullable public AddAppFragment       addAppFragment;
		@Nullable public AppOverviewFragment  appOverviewFragment;
		@Nullable public PreferencesFragment  preferencesFragment;
		@Nullable public MainActivity         mainActivity;
		private          Context              context;
		private volatile boolean              shallInitalize = true;
		
		public void initalizeIfNotYetInitalized(Context applicationContext) {
			if (shallInitalize) {
				context = applicationContext;
				AppList.load();
				
				shallInitalize = false;
			}
		}
		
		@NonNull
		public Context getContext() {
			if (context == null)
				throw new IllegalStateException("There should always be a context set");
			return context;
		}
		
		public void setContext(Context context) {
			this.context = context;
		}
	}
}
