package net.osmand.plus.settings.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.profiles.onlinerouting.OnlineRoutingSegmentCard;
import net.osmand.plus.profiles.onlinerouting.OnlineRoutingSegmentCard.OnTextChangedListener;
import net.osmand.plus.profiles.onlinerouting.ServerType;
import net.osmand.plus.profiles.onlinerouting.ExampleLocation;
import net.osmand.plus.profiles.onlinerouting.VehicleType;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class OnlineRoutingEngineFragment extends BaseOsmAndFragment {

	public static final String TAG = OnlineRoutingEngineFragment.class.getSimpleName();

	private static final String ENGINE_NAME_KEY = "engine_name";
	private static final String ENGINE_SERVER_KEY = "engine_server";
	private static final String ENGINE_SERVER_URL_KEY = "engine_server_url";
	private static final String ENGINE_VEHICLE_TYPE_KEY = "engine_vehicle_type";
	private static final String ENGINE_CUSTOM_VEHICLE_KEY = "engine_custom_vehicle";
	private static final String ENGINE_API_KEY_KEY = "engine_api_key";
	private static final String ENGINE_NAME_CHANGED_BY_USER_KEY = "engine_name_changed_by_user_key";
	private static final String EXAMPLE_LOCATION_KEY = "example_location";

	private OnlineRoutingSegmentCard nameCard;
	private OnlineRoutingSegmentCard serverCard;
	private OnlineRoutingSegmentCard vehicleCard;
	private OnlineRoutingSegmentCard apiKeyCard;
	private OnlineRoutingSegmentCard exampleCard;

	private boolean isEditingMode;
	private boolean nightMode;

	private OnlineRoutingEngineObject engine;
	private ExampleLocation selectedLocation;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		engine = new OnlineRoutingEngineObject();
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else {
			initEngineState();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		nightMode = !app.getSettings().isLightContent();

		View view = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.online_routing_engine_preference, container, false);

		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(getContext(), view);
		}
		setupToolbar(view);

		ViewGroup segmentsContainer = (ViewGroup) view.findViewById(R.id.segments_container);

		// create name card
		nameCard = new OnlineRoutingSegmentCard(mapActivity);
		nameCard.setDescription(getString(R.string.select_nav_profile_dialog_message));
		nameCard.setFieldBoxLabelText(getString(R.string.shared_string_name));
		nameCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean changedByUser, String text) {
				if (!isEditingMode && !engine.wasNameChangedByUser && changedByUser) {
					engine.wasNameChangedByUser = true;
				}
				engine.name = text;
			}
		});
		nameCard.showDivider();
		segmentsContainer.addView(nameCard.getView());

		// create server card
		serverCard = new OnlineRoutingSegmentCard(mapActivity);
		serverCard.setHeaderTitle(getString(R.string.shared_string_type));
		List<HorizontalSelectionItem> serverItems = new ArrayList<>();
		for (ServerType server : ServerType.values()) {
			serverItems.add(new HorizontalSelectionItem(server.getTitle(), server));
		}
		serverCard.setSelectionMenu(serverItems, engine.serverType.getTitle(),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						ServerType server = (ServerType) result.getObject();
						if (engine.serverType != server) {
							engine.serverType = server;
							updateCardViews(nameCard, serverCard, exampleCard);
							return true;
						}
						return false;
					}
				});
//		serverCard.setOnTextChangedListener(new OnTextChangedListener() {
//			@Override
//			public void onTextChanged(boolean editedByUser, String text) {
//				engine.serverBaseUrl = text;
//			}
//		});
		serverCard.setFieldBoxLabelText(getString(R.string.shared_string_server_url));
		serverCard.showDivider();
		segmentsContainer.addView(serverCard.getView());

		// create vehicle card
		vehicleCard = new OnlineRoutingSegmentCard(mapActivity);
		vehicleCard.setHeaderTitle(getString(R.string.shared_string_vehicle));
		List<HorizontalSelectionItem> vehicleItems = new ArrayList<>();
		for (VehicleType vehicle : VehicleType.values()) {
			vehicleItems.add(new HorizontalSelectionItem(vehicle.getTitle(), vehicle));
		}
		vehicleCard.setSelectionMenu(vehicleItems, engine.vehicleType.getTitle(),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						VehicleType vehicle = (VehicleType) result.getObject();
						if (engine.vehicleType != vehicle) {
							engine.vehicleType = vehicle;
							updateCardViews(nameCard, vehicleCard, exampleCard);
							return true;
						}
						return false;
					}
				});
		vehicleCard.setFieldBoxLabelText(getString(R.string.shared_string_custom));
		vehicleCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean editedByUser, String text) {
				engine.customVehicleKey = text;
			}
		});
		vehicleCard.setEditedText(engine.customVehicleKey);
		vehicleCard.setFieldBoxHelperText(getString(R.string.shared_string_enter_param));
		vehicleCard.showDivider();
		segmentsContainer.addView(vehicleCard.getView());

		// create api key card
		apiKeyCard = new OnlineRoutingSegmentCard(mapActivity);
		apiKeyCard.setHeaderTitle(getString(R.string.shared_string_api_key));
		apiKeyCard.setFieldBoxLabelText(getString(R.string.keep_it_empty_if_not));
		apiKeyCard.setEditedText(engine.apiKey);
		apiKeyCard.showDivider();
		apiKeyCard.setOnTextChangedListener(new OnTextChangedListener() {
			@Override
			public void onTextChanged(boolean editedByUser, String text) {
				engine.apiKey = text;
				updateCardViews(exampleCard);
			}
		});
		segmentsContainer.addView(apiKeyCard.getView());

		// create example card
		exampleCard = new OnlineRoutingSegmentCard(mapActivity);
		exampleCard.setHeaderTitle(getString(R.string.shared_string_example));
		List<HorizontalSelectionItem> locationItems = new ArrayList<>();
		for (ExampleLocation location : ExampleLocation.values()) {
			locationItems.add(new HorizontalSelectionItem(location.getTitle(), location));
		}
		exampleCard.setSelectionMenu(locationItems, selectedLocation.getTitle(),
				new CallbackWithObject<HorizontalSelectionItem>() {
					@Override
					public boolean processResult(HorizontalSelectionItem result) {
						ExampleLocation location = (ExampleLocation) result.getObject();
						if (selectedLocation != location) {
							selectedLocation = location;
							updateCardViews(exampleCard);
							return true;
						}
						return false;
					}
				});
		exampleCard.setFieldBoxHelperText(getString(R.string.online_routing_example_hint));
		exampleCard.setButton(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// make request to the server
			}
		});
		segmentsContainer.addView(exampleCard.getView());

		View bottomSpaceView = new View(app);
		int space = (int) getResources().getDimension(R.dimen.empty_state_text_button_padding_top);
		bottomSpaceView.setLayoutParams(
				new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		segmentsContainer.addView(bottomSpaceView);

		View cancelButton = view.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, cancelButton,
				DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		view.findViewById(R.id.buttons_divider).setVisibility(View.VISIBLE);

		View saveButton = view.findViewById(R.id.right_bottom_button);
		UiUtilities.setupDialogButton(nightMode, saveButton,
				UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_save);
		saveButton.setVisibility(View.VISIBLE);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// save engine to settings
			}
		});

		updateCardViews(nameCard, serverCard, vehicleCard, exampleCard);
		return view;
	}

	private void setupToolbar(View mainView) {
		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		ImageView navigationIcon = toolbar.findViewById(R.id.close_button);
		navigationIcon.setImageResource(R.drawable.ic_action_close);
		navigationIcon.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		TextView title = toolbar.findViewById(R.id.toolbar_title);
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);
		View actionBtn = toolbar.findViewById(R.id.action_button);
		if (isEditingMode) {
			title.setText(getString(R.string.edit_online_routing_engine));
			ImageView ivBtn = toolbar.findViewById(R.id.action_button_icon);
			ivBtn.setImageDrawable(
					getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));
			actionBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// delete engine from settings
				}
			});
		} else {
			title.setText(getString(R.string.add_online_routing_engine));
			actionBtn.setVisibility(View.GONE);
		}
	}

	private void updateCardViews(BaseCard ... cardsToUpdate) {
		for (BaseCard card : cardsToUpdate) {
			if (nameCard.equals(card)) {
				if (!engine.wasNameChangedByUser) {
					String name = String.format(
							getString(R.string.ltr_or_rtl_combine_via_dash),
							engine.serverType.getTitle(), engine.vehicleType.getTitle());
					nameCard.setEditedText(name);
				}

			} else if (serverCard.equals(card)) {
				serverCard.setHeaderSubtitle(engine.serverType.getTitle());
				serverCard.setEditedText(engine.serverType.getBaseUrl());

			} else if (vehicleCard.equals(card)) {
				vehicleCard.setHeaderSubtitle(engine.vehicleType.getTitle());
				if (engine.vehicleType == VehicleType.CUSTOM) {
					vehicleCard.showFieldBox();
				} else {
					vehicleCard.hideFieldBox();
				}

			} else if (exampleCard.equals(card)) {
				exampleCard.setEditedText(getTestUrl());
			}
		}
	}

	private String getTestUrl() {
		String baseUrl = engine.serverType.getBaseUrl();
		String vehicle = engine.getVehicleKey();

		LatLon kievLatLon = new LatLon(50.431759, 30.517023);
		LatLon destinationLatLon = selectedLocation.getLatLon();

		if (engine.serverType == ServerType.GRAPHHOPER) {
			return baseUrl + "point=" + kievLatLon.getLatitude()
					+ "," + kievLatLon.getLongitude()
					+ "&" + "point=" + destinationLatLon.getLatitude()
					+ "," + destinationLatLon.getLongitude()
					+ "&" + "vehicle=" + vehicle
					+ (!Algorithms.isEmpty(engine.apiKey) ? ("&" + "key=" + engine.apiKey) : "");
		} else {
			return baseUrl + vehicle + "/" + kievLatLon.getLatitude()
					+ "," + kievLatLon.getLongitude()
					+ ";" + destinationLatLon.getLatitude()
					+ "," + destinationLatLon.getLongitude()
					+ "?geometries=geojson&overview=full";
		}
	}

	public static void showInstance(FragmentActivity activity, boolean isEditingMode) {
		OnlineRoutingEngineFragment fragment = new OnlineRoutingEngineFragment();
		fragment.isEditingMode = isEditingMode;
		activity.getSupportFragmentManager().beginTransaction()
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commitAllowingStateLoss();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState(outState);
	}

	private void saveState(Bundle outState) {
		outState.putString(ENGINE_NAME_KEY, engine.name);
		outState.putString(ENGINE_SERVER_KEY, engine.serverType.name());
		outState.putString(ENGINE_SERVER_URL_KEY, engine.serverBaseUrl);
		outState.putString(ENGINE_VEHICLE_TYPE_KEY, engine.vehicleType.name());
		outState.putString(ENGINE_CUSTOM_VEHICLE_KEY, engine.customVehicleKey);
		outState.putString(ENGINE_API_KEY_KEY, engine.apiKey);
		outState.putBoolean(ENGINE_NAME_CHANGED_BY_USER_KEY, engine.wasNameChangedByUser);
		outState.putString(EXAMPLE_LOCATION_KEY, selectedLocation.name());
	}

	private void restoreState(Bundle savedState) {
		engine.name = savedState.getString(ENGINE_NAME_KEY);
		engine.serverType = ServerType.valueOf(savedState.getString(ENGINE_SERVER_KEY));
		engine.serverBaseUrl = savedState.getString(ENGINE_SERVER_URL_KEY);
		engine.vehicleType = VehicleType.valueOf(savedState.getString(ENGINE_VEHICLE_TYPE_KEY));
		engine.customVehicleKey = savedState.getString(ENGINE_CUSTOM_VEHICLE_KEY);
		engine.apiKey = savedState.getString(ENGINE_API_KEY_KEY);
		engine.wasNameChangedByUser = savedState.getBoolean(ENGINE_NAME_CHANGED_BY_USER_KEY);
		selectedLocation = ExampleLocation.valueOf(savedState.getString(EXAMPLE_LOCATION_KEY));
	}


	private void initEngineState() {
		engine.serverType = ServerType.values()[0];
		engine.vehicleType = VehicleType.values()[0];
		selectedLocation = ExampleLocation.values()[0];
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	private static class OnlineRoutingEngineObject {
		private String name;
		private ServerType serverType;
		private String serverBaseUrl;
		private VehicleType vehicleType;
		private String customVehicleKey;
		private String apiKey;
		private boolean wasNameChangedByUser;

		public String getVehicleKey() {
			if (vehicleType == VehicleType.CUSTOM) {
				return customVehicleKey;
			}
			return vehicleType.getKey();
		}
	}
}
