package cm.aptoide.pt.v8engine.view.account;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.presenter.LoginSignUpPresenter;
import cm.aptoide.pt.v8engine.presenter.LoginSignUpView;
import cm.aptoide.pt.v8engine.view.BackButtonFragment;
import cm.aptoide.pt.v8engine.view.navigator.FragmentNavigator;

/**
 * This fragment has too much code equal to {@link LoginSignUpCredentialsFragment} due to Google /
 * Facebook
 * login functionality. Further code refactoring is needed to migrate external source login into
 * their own fragment and include the fragment inside the necessary login / sign up views.
 */
public class LoginSignUpFragment extends BackButtonFragment implements LoginSignUpView {

  private static final String TAG = LoginSignUpFragment.class.getName();

  private static final String BOTTOM_SHEET_WITH_BOTTOM_BAR = "bottom_sheet_expanded";
  private static final String DISMISS_TO_NAVIGATE_TO_MAIN_VIEW = "dismiss_to_navigate_to_main_view";
  private static final String NAVIGATE_TO_HOME = "clean_back_stack";
  private static final String ACCOUNT_TYPE = "account_type";
  private static final String AUTH_TYPE = "auth_type";
  private static final String IS_NEW_ACCOUNT = "is_new_account";

  private BottomSheetBehavior<View> bottomSheetBehavior;
  private boolean withBottomBar;
  private boolean dismissToNavigateToMainView;
  private boolean navigateToHome;
  private FragmentNavigator navigator;
  private ClickHandler backClickHandler;
  private LoginBottomSheet loginBottomSheet;

  public static LoginSignUpFragment newInstance(boolean withBottomBar,
      boolean dismissToNavigateToMainView, boolean navigateToHome) {
    return newInstance(withBottomBar, dismissToNavigateToMainView, navigateToHome, "", "", true);
  }

  public static LoginSignUpFragment newInstance(boolean withBottomBar,
      boolean dismissToNavigateToMainView, boolean navigateToHome, String accountType,
      String authType, boolean isNewAccount) {
    Bundle args = new Bundle();
    args.putBoolean(BOTTOM_SHEET_WITH_BOTTOM_BAR, withBottomBar);
    args.putBoolean(DISMISS_TO_NAVIGATE_TO_MAIN_VIEW, dismissToNavigateToMainView);
    args.putBoolean(NAVIGATE_TO_HOME, navigateToHome);
    args.putString(ACCOUNT_TYPE, accountType);
    args.putString(AUTH_TYPE, authType);
    args.putBoolean(IS_NEW_ACCOUNT, isNewAccount);

    LoginSignUpFragment fragment = new LoginSignUpFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof LoginBottomSheet) {
      loginBottomSheet = (LoginBottomSheet) context;
    } else {
      throw new IllegalStateException(
          "Context should implement " + LoginBottomSheet.class.getSimpleName());
    }
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    navigator = getFragmentChildNavigator(R.id.login_signup_layout);
    final Bundle args = getArguments();
    withBottomBar = args.getBoolean(BOTTOM_SHEET_WITH_BOTTOM_BAR);
    dismissToNavigateToMainView = args.getBoolean(DISMISS_TO_NAVIGATE_TO_MAIN_VIEW);
    navigateToHome = args.getBoolean(NAVIGATE_TO_HOME);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    backClickHandler = new ClickHandler() {
      @Override public boolean handle() {
        if (bottomSheetBehavior != null
            && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
          bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
          return true;
        }
        return false;
      }
    };
    registerBackClickHandler(backClickHandler);
    bindViews(view);
    attachPresenter(new LoginSignUpPresenter(this), savedInstanceState);
  }

  @Override public void onDestroyView() {
    if (bottomSheetBehavior != null) {
      bottomSheetBehavior.setBottomSheetCallback(null);
      bottomSheetBehavior = null;
    }
    unregisterBackClickHandler(backClickHandler);
    super.onDestroyView();
  }

  private void bindViews(View view) {
    try {
      bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.login_signup_layout));
    } catch (IllegalArgumentException ex) {
      // this happens because in landscape the R.id.login_signup_layout is not
      // a child of CoordinatorLayout
    }

    if (bottomSheetBehavior != null) {
      // pass some of this logic to the presenter
      final View mainContent = view.findViewById(R.id.main_content);
      final int originalBottomPadding = withBottomBar ? mainContent.getPaddingBottom() : 0;
      if (withBottomBar) {
        view.findViewById(R.id.appbar)
            .setVisibility(View.GONE);
      } else {
        view.findViewById(R.id.appbar)
            .setVisibility(View.VISIBLE);
        setupToolbar(view, getString(R.string.my_account));
      }
      mainContent.setPadding(0, 0, 0, originalBottomPadding);

      bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
      bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
        @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
          switch (newState) {
            case BottomSheetBehavior.STATE_COLLAPSED:
              loginBottomSheet.collapse();
              mainContent.setPadding(0, 0, 0, originalBottomPadding);
              break;
            case BottomSheetBehavior.STATE_EXPANDED:
              loginBottomSheet.expand();
              mainContent.setPadding(0, 0, 0, 0);
              break;
          }
        }

        @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
      });
    }
  }

  protected Toolbar setupToolbar(View view, String title) {
    setHasOptionsMenu(true);
    Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
    toolbar.setLogo(R.drawable.logo_toolbar);

    toolbar.setTitle(title);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

    ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);

    return toolbar;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View view = inflater.inflate(getLayoutId(), container, false);

    LoginSignUpCredentialsFragment fragment = null;
    try {
      fragment = (LoginSignUpCredentialsFragment) navigator.getFragment();
    } catch (ClassCastException ex) {
      Logger.e(TAG, ex);
    }

    if (fragment == null) {
      navigator.navigateToWithoutBackSave(
          LoginSignUpCredentialsFragment.newInstance(dismissToNavigateToMainView, navigateToHome));
    }

    return view;
  }

  @LayoutRes public int getLayoutId() {
    return R.layout.fragment_login_sign_up;
  }
}
