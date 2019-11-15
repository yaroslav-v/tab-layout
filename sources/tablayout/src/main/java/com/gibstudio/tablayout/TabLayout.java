package com.gibstudio.tablayout;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.core.view.GravityCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.appcompat.widget.AppCompatDrawableManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public class TabLayout extends HorizontalScrollView {

    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final int MAX_TAB_TEXT_LINES = 2;

    private static final int DEFAULT_HEIGHT = 45; // dps
    private static final int TAB_MIN_WIDTH_MARGIN = 56; //dps
    private static final int FIXED_WRAP_GUTTER_MIN = 16; //dps
    private static final int MOTION_NON_ADJACENT_OFFSET = 24;

    private static final int ANIMATION_DURATION = 300;

    /**
     * Scrollable tabs display a subset of tabs at any given moment, and can contain longer tab
     * labels and a larger number of tabs. They are best used for browsing contexts in touch
     * interfaces when users don’t need to directly compare the tab labels.
     *
     * @attr android.support.design.R.attr.tabMode
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_SCROLLABLE = 0;

    /**
     * Fixed tabs display all tabs concurrently and are best used with content that benefits from
     * quick pivots between tabs. The maximum number of tabs is limited by the view’s width.
     * Fixed tabs have equal width, based on the widest tab label.
     *
     * @attr android.support.design.R.attr.tabMode
     * @see #setTabMode(int)
     * @see #getTabMode()
     */
    public static final int MODE_FIXED = 1;

    /**
     * Gravity used to fill the {@link TabLayout} as much as possible. This option only takes effect
     * when used with {@link #MODE_FIXED}.
     *
     * @attr android.support.design.R.attr.tabGravity
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_FILL = 0;

    /**
     * Gravity used to lay out the tabs in the center of the {@link TabLayout}.
     *
     * @attr android.support.design.R.attr.tabGravity
     * @see #setTabGravity(int)
     * @see #getTabGravity()
     */
    public static final int GRAVITY_CENTER = 1;

    /**
     * Orientation of tabs content
     *
     * @see #setTabOrientation(int)
     * @see #getTabOrientation()
     */
    public static final int HORIZONTAL = 0;

    /**
     * Orientation of tabs content
     *
     * @see #setTabOrientation(int)
     * @see #getTabOrientation()
     */
    public static final int VERTICAL = 1;

    /**
     * Callback interface invoked when a tab's selection state changes.
     */
    public interface OnTabSelectedListener {

        /**
         * Called when a tab enters the selected state.
         *
         * @param tab The tab that was selected
         */
        public void onTabSelected(Tab tab);

        /**
         * Called when a tab exits the selected state.
         *
         * @param tab The tab that was unselected
         */
        public void onTabUnselected(Tab tab);

        /**
         * Called when a tab that is already selected is chosen again by the user. Some applications
         * may use this action to return to the top level of a category.
         *
         * @param tab The tab that was reselected.
         */
        public void onTabReselected(Tab tab);
    }

    /**
     * Callback interface invoked when a tab's animation state changes.
     */
    public interface OnTabAnimationListener {
        /**
         * Called when a tab animation started.
         *
         * @param tab The tab that was unselected
         */
        public void onTabAnimationStart(Tab tab);

        /**
         * Called when a tab animation finished.
         *
         * @param tab The tab that was selected
         */
        public void onTabAnimationEnd(Tab tab);
    }

    private final ArrayList<Tab> mTabs = new ArrayList<>();
    private Tab mSelectedTab;

    private final SlidingTabStrip mTabStrip;

    private int mTabPaddingStart;
    private int mTabPaddingTop;
    private int mTabPaddingEnd;
    private int mTabPaddingBottom;
    private int mTabPaddingInternal;

    private final int mTabTextAppearance;
    private int mTabSelectedTextColor;
    private boolean mTabSelectedTextColorSet;
    private final int mTabBackgroundResId;

    private final int mTabMinWidth;
    private int mTabMaxWidth;
    private final int mRequestedTabMaxWidth;

    private int mContentInsetStart;

    private int mMode;
    private int mTabGravity;
    private int mTabOrientation;

    private OnTabSelectedListener mOnTabSelectedListener;
    private OnTabAnimationListener mOnTabAnimationListener;
    private OnClickListener mTabClickListener;

    public TabLayout(Context context) {
        this(context, null);
    }

    public TabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Set us to fill the View port
        setFillViewport(true);

        // Add the TabStrip
        mTabStrip = new SlidingTabStrip(context);
        addView(mTabStrip, LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabLayout,
                defStyleAttr, R.style.Widget_Design_TabLayout);

        mTabStrip.setSelectedIndicatorGravity(a.getInt(R.styleable.TabLayout_tabIndicatorGravity,
                SlidingTabStrip.GRAVITY_BOTTOM));
        mTabStrip.setSelectedIndicatorHeight(
                a.getDimensionPixelSize(R.styleable.TabLayout_tabIndicatorHeight, 0));
        mTabStrip.setSelectedIndicatorColor(a.getColor(R.styleable.TabLayout_tabIndicatorColor, 0));
        mTabStrip.setSelectedIndicatorScrollable(a.getBoolean(R.styleable.TabLayout_tabIndicatorScrollable, true));

        mTabTextAppearance = a.getResourceId(R.styleable.TabLayout_tabTextAppearance,
                R.style.TextAppearance_Design_Tab);

        mTabPaddingStart = mTabPaddingTop = mTabPaddingEnd = mTabPaddingBottom = mTabPaddingInternal =
                a.getDimensionPixelSize(R.styleable.TabLayout_tabPadding, 0);
        mTabPaddingStart = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingStart,
                mTabPaddingStart);
        mTabPaddingTop = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingTop,
                mTabPaddingTop);
        mTabPaddingEnd = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingEnd,
                mTabPaddingEnd);
        mTabPaddingBottom = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingBottom,
                mTabPaddingBottom);
        mTabPaddingInternal = a.getDimensionPixelSize(R.styleable.TabLayout_tabPaddingInternal,
                mTabPaddingInternal);

        if (a.hasValue(R.styleable.TabLayout_tabSelectedTextColor)) {
            mTabSelectedTextColor = a.getColor(R.styleable.TabLayout_tabSelectedTextColor, 0);
            mTabSelectedTextColorSet = true;
        }

        mTabMinWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMinWidth, 0);
        mRequestedTabMaxWidth = a.getDimensionPixelSize(R.styleable.TabLayout_tabMaxWidth, 0);
        mTabBackgroundResId = a.getResourceId(R.styleable.TabLayout_tabBackground, 0);
        mContentInsetStart = a.getDimensionPixelSize(R.styleable.TabLayout_tabContentStart, 0);
        mMode = a.getInt(R.styleable.TabLayout_tabMode, MODE_FIXED);
        mTabGravity = a.getInt(R.styleable.TabLayout_tabGravity, GRAVITY_FILL);
        mTabOrientation = a.getInt(R.styleable.TabLayout_tabOrientation, HORIZONTAL);
        a.recycle();

        // Now apply the tab mode and gravity
        applyModeAndGravity();
    }

    /**
     * Sets the tab indicator's color for the currently selected tab.
     *
     * @param color color to use for the indicator
     * @attr ref android.support.design.R.styleable#TabLayout_tabIndicatorColor
     */
    public void setSelectedTabIndicatorColor(@ColorInt int color) {
        mTabStrip.setSelectedIndicatorColor(color);
    }

    /**
     * Sets the tab indicator's height for the currently selected tab.
     *
     * @param height height to use for the indicator in pixels
     * @attr ref android.support.design.R.styleable#TabLayout_tabIndicatorHeight
     */
    public void setSelectedTabIndicatorHeight(int height) {
        mTabStrip.setSelectedIndicatorHeight(height);
    }

    /**
     * Set the scroll position of the tabs. This is useful for when the tabs are being displayed as
     * part of a scrolling container such as {@link ViewPager}.
     * <p/>
     * Calling this method does not update the selected tab, it is only used for drawing purposes.
     */
    public void setScrollPosition(int position, float positionOffset) {
        if (isAnimationRunning(getAnimation())) {
            return;
        }
        if (position < 0 || position >= mTabStrip.getChildCount()) {
            return;
        }

        // Set the indicator position and update the scroll to match
        mTabStrip.setIndicatorPositionFromTabPosition(position, positionOffset);
        scrollTo(calculateScrollXForTab(position, positionOffset), 0);

        // Update the 'selected state' view as we scroll
        setSelectedTabView(Math.round(position + positionOffset));
    }

    /**
     * Add new {@link Tab}s populated from a {@link PagerAdapter}. Each tab will have it's text set
     * to the value returned from {@link PagerAdapter#getPageTitle(int)}.
     *
     * @param adapter the adapter to populate from
     */
    public void addTabsFromPagerAdapter(PagerAdapter adapter) {
        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            addTab(newTab().setText(adapter.getPageTitle(i)));
        }
    }

    public void setupWithViewPager(ViewPager viewPager) {
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            throw new IllegalArgumentException("ViewPager does not have a PagerAdapter set");
        } else {
            this.addTabsFromPagerAdapter(adapter);
            viewPager.setOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(this));
            this.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
            if (this.mSelectedTab == null || this.mSelectedTab.getPosition() != viewPager.getCurrentItem()) {
                this.getTabAt(viewPager.getCurrentItem()).select();
            }

        }
    }

    public static class ViewPagerOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
        private final ViewPager mViewPager;

        public ViewPagerOnTabSelectedListener(ViewPager viewPager) {
            this.mViewPager = viewPager;
        }

        public void onTabSelected(TabLayout.Tab tab) {
            this.mViewPager.setCurrentItem(tab.getPosition());
        }

        public void onTabUnselected(TabLayout.Tab tab) {
        }

        public void onTabReselected(TabLayout.Tab tab) {
        }
    }

    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListener(TabLayout tabLayout) {
            this.mTabLayoutRef = new WeakReference(tabLayout);
        }

        public void onPageScrollStateChanged(int state) {
            this.mPreviousScrollState = this.mScrollState;
            this.mScrollState = state;
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            TabLayout tabLayout = (TabLayout) this.mTabLayoutRef.get();
            if (tabLayout != null) {
                tabLayout.setScrollPosition(position, positionOffset);
            }

        }

        public void onPageSelected(int position) {
            TabLayout tabLayout = (TabLayout) this.mTabLayoutRef.get();
            if (tabLayout != null) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        }
    }

    /**
     * Create a {@link ViewPager.OnPageChangeListener} which implements the
     * necessary calls back to this layout so that the tabs position is kept in sync.
     * <p/>
     * If you need to have a custom {@link ViewPager.OnPageChangeListener} for your own
     * purposes, you can still use the instance returned from this method, but making sure to call
     * through to all of the methods.
     */
    public ViewPager.OnPageChangeListener createOnPageChangeListener() {
        return new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                setScrollPosition(position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                getTabAt(position).select();
            }
        };
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab Tab to add
     */
    public void addTab(Tab tab) {
        addTab(tab, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     * If this is the first tab to be added it will become the selected tab.
     *
     * @param tab      The tab to add
     * @param position The new position of the tab
     */
    public void addTab(Tab tab, int position) {
        addTab(tab, position, mTabs.isEmpty());
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list.
     *
     * @param tab         Tab to add
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(Tab tab, boolean setSelected) {
        if (tab.mParent != this) {
            throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
        }

        addTabView(tab, setSelected);
        configureTab(tab, mTabs.size());
        if (setSelected) {
            tab.select();
        }
    }

    /**
     * Add a tab to this layout. The tab will be inserted at <code>position</code>.
     *
     * @param tab         The tab to add
     * @param position    The new position of the tab
     * @param setSelected True if the added tab should become the selected tab.
     */
    public void addTab(Tab tab, int position, boolean setSelected) {
        if (tab.mParent != this) {
            throw new IllegalArgumentException("Tab belongs to a different TabLayout.");
        }

        addTabView(tab, position, setSelected);
        configureTab(tab, position);
        if (setSelected) {
            tab.select();
        }
    }

    /**
     * Set the {@link com.google.android.material.tabs.TabLayout.OnTabSelectedListener} that will handle switching to and from tabs.
     *
     * @param onTabSelectedListener Listener to handle tab selection events
     */
    public void setOnTabSelectedListener(OnTabSelectedListener onTabSelectedListener) {
        mOnTabSelectedListener = onTabSelectedListener;
    }

    /**
     * Set the listener that will handle tabs animation.
     *
     * @param onTabAnimationListener Listener to handle tab selection events
     */
    public void setOnTabAnimationListener(OnTabAnimationListener onTabAnimationListener) {
        mOnTabAnimationListener = onTabAnimationListener;
    }

    /**
     * Create and return a new {@link Tab}. You need to manually add this using
     * {@link #addTab(Tab)} or a related method.
     *
     * @return A new Tab
     * @see #addTab(Tab)
     */
    public Tab newTab() {
        return new Tab(this);
    }

    /**
     * Returns the number of tabs currently registered with the action bar.
     *
     * @return Tab count
     */
    public int getTabCount() {
        return mTabs.size();
    }

    /**
     * Returns the tab at the specified index.
     */
    public Tab getTabAt(int index) {
        return mTabs.get(index);
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param tab The tab to remove
     */
    public void removeTab(Tab tab) {
        if (tab.mParent != this) {
            throw new IllegalArgumentException("Tab does not belong to this TabLayout.");
        }

        removeTabAt(tab.getPosition());
    }

    /**
     * Remove a tab from the layout. If the removed tab was selected it will be deselected
     * and another tab will be selected if present.
     *
     * @param position Position of the tab to remove
     */
    public void removeTabAt(int position) {
        final int selectedTabPosition = mSelectedTab != null ? mSelectedTab.getPosition() : 0;
        removeTabViewAt(position);

        Tab removedTab = mTabs.remove(position);
        if (removedTab != null) {
            removedTab.setPosition(Tab.INVALID_POSITION);
        }

        final int newTabCount = mTabs.size();
        for (int i = position; i < newTabCount; i++) {
            mTabs.get(i).setPosition(i);
        }

        if (selectedTabPosition == position) {
            selectTab(mTabs.isEmpty() ? null : mTabs.get(Math.max(0, position - 1)));
        }
    }

    /**
     * Remove all tabs from the action bar and deselect the current tab.
     */
    public void removeAllTabs() {
        // Remove all the views
        mTabStrip.removeAllViews();

        for (Iterator<Tab> i = mTabs.iterator(); i.hasNext(); ) {
            Tab tab = i.next();
            tab.setPosition(Tab.INVALID_POSITION);
            i.remove();
        }
    }

    /**
     * Set the behavior mode for the Tabs in this layout. The valid input options are:
     * <ul>
     * <li>{@link #MODE_FIXED}: Fixed tabs display all tabs concurrently and are best used
     * with content that benefits from quick pivots between tabs.</li>
     * <li>{@link #MODE_SCROLLABLE}: Scrollable tabs display a subset of tabs at any given moment,
     * and can contain longer tab labels and a larger number of tabs. They are best used for
     * browsing contexts in touch interfaces when users don’t need to directly compare the tab
     * labels. This mode is commonly used with a {@link ViewPager}.</li>
     * </ul>
     *
     * @param mode one of {@link #MODE_FIXED} or {@link #MODE_SCROLLABLE}.
     */
    public void setTabMode(int mode) {
        if (mode != mMode) {
            mMode = mode;
            applyModeAndGravity();
        }
    }

    /**
     * Returns the current mode used by this {@link TabLayout}.
     *
     * @see #setTabMode(int)
     */
    public int getTabMode() {
        return mMode;
    }

    /**
     * Set the gravity to use when laying out the tabs.
     *
     * @param gravity one of {@link #GRAVITY_CENTER} or {@link #GRAVITY_FILL}.
     */
    public void setTabGravity(int gravity) {
        if (mTabGravity != gravity) {
            mTabGravity = gravity;
            applyModeAndGravity();
        }
    }

    /**
     * The current gravity used for laying out tabs.
     *
     * @return one of {@link #GRAVITY_CENTER} or {@link #GRAVITY_FILL}.
     */
    public int getTabGravity() {
        return mTabGravity;
    }

    /**
     * Set the orientation to use when laying out the tabs.
     *
     * @param orientation one of {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public void setTabOrientation(int orientation) {
        if (mTabOrientation != orientation) {
            mTabOrientation = orientation;
            updateTabViewsOrientation();
        }
    }

    /**
     * The current orientation used for laying out tabs.
     *
     * @return one of {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    public int getTabOrientation() {
        return mTabOrientation;
    }

    /**
     * Set the text color to use when a tab is selected.
     *
     * @param textColor
     */
    public void setTabSelectedTextColor(@ColorInt int textColor) {
        if (!mTabSelectedTextColorSet || mTabSelectedTextColor != textColor) {
            mTabSelectedTextColor = textColor;
            mTabSelectedTextColorSet = true;

            for (int i = 0, z = mTabStrip.getChildCount(); i < z; i++) {
                updateTab(i);
            }
        }
    }

    /**
     * Returns the text color currently used when a tab is selected.
     */
    public int getTabSelectedTextColor() {
        return mTabSelectedTextColor;
    }

    private TabView createTabView(Tab tab) {
        final TabView tabView = new TabView(getContext(), tab);
        tabView.setFocusable(true);

        if (mTabClickListener == null) {
            mTabClickListener = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    TabView tabView = (TabView) view;
                    tabView.getTab().select();
                }
            };
        }
        tabView.setOnClickListener(mTabClickListener);
        return tabView;
    }

    private void configureTab(Tab tab, int position) {
        tab.setPosition(position);
        mTabs.add(position, tab);

        final int count = mTabs.size();
        for (int i = position + 1; i < count; i++) {
            mTabs.get(i).setPosition(i);
        }
    }

    private void updateTab(int position) {
        final TabView view = (TabView) mTabStrip.getChildAt(position);
        if (view != null) {
            view.update();
        }
    }

    private void addTabView(Tab tab, boolean setSelected) {
        final TabView tabView = createTabView(tab);
        mTabStrip.addView(tabView, createLayoutParamsForTabs());
        if (setSelected) {
            tabView.setSelected(true);
        }
    }

    private void addTabView(Tab tab, int position, boolean setSelected) {
        final TabView tabView = createTabView(tab);
        mTabStrip.addView(tabView, position, createLayoutParamsForTabs());
        if (setSelected) {
            tabView.setSelected(true);
        }
    }

    private LinearLayout.LayoutParams createLayoutParamsForTabs() {
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        updateTabViewLayoutParams(lp);
        return lp;
    }

    private void updateTabViewLayoutParams(LinearLayout.LayoutParams lp) {
        if (mMode == MODE_FIXED && mTabGravity == GRAVITY_FILL) {
            lp.width = 0;
            lp.weight = 1;
        } else {
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 0;
        }
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // If we have a MeasureSpec which allows us to decide our height, try and use the default
        // height
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.min(dpToPx(DEFAULT_HEIGHT), MeasureSpec.getSize(heightMeasureSpec)),
                        MeasureSpec.EXACTLY);
                break;
            case MeasureSpec.UNSPECIFIED:
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(dpToPx(DEFAULT_HEIGHT),
                        MeasureSpec.EXACTLY);
                break;
        }

        // Now super measure itself using the (possibly) modified height spec
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mMode == MODE_FIXED && getChildCount() == 1) {
            // If we're in fixed mode then we need to make the tab strip is the same width as us
            // so we don't scroll
            final View child = getChildAt(0);
            final int width = getMeasuredWidth();

            if (child.getMeasuredWidth() > width) {
                // If the child is wider than us, re-measure it with a widthSpec set to exact our
                // measure width
                int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop()
                        + getPaddingBottom(), child.getLayoutParams().height);
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        // Now update the tab max width. We do it here as the default tab min width is
        // layout width - 56dp
        int maxTabWidth = mRequestedTabMaxWidth;
        final int defaultTabMaxWidth = getMeasuredWidth() - dpToPx(TAB_MIN_WIDTH_MARGIN);
        if (maxTabWidth == 0 || maxTabWidth > defaultTabMaxWidth) {
            // If the request tab max width is 0, or larger than our default, use the default
            maxTabWidth = defaultTabMaxWidth;
        }
        mTabMaxWidth = maxTabWidth;
    }

    private void removeTabViewAt(int position) {
        mTabStrip.removeViewAt(position);
        requestLayout();
    }

    private void animateToTab(final int newPosition) {
        clearAnimation();

        if (newPosition == Tab.INVALID_POSITION) {
            return;
        }

        if (getWindowToken() == null || !ViewCompat.isLaidOut(this)) {
            // If we don't have a window token, or we haven't been laid out yet just draw the new
            // position now
            setScrollPosition(newPosition, 0f);
            return;
        }

        final int startScrollX = getScrollX();
        final int targetScrollX = calculateScrollXForTab(newPosition, 0);
        final int duration = ANIMATION_DURATION;

        if (startScrollX != targetScrollX) {
            final Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    final float value = lerp(startScrollX, targetScrollX, interpolatedTime);
                    scrollTo((int) value, 0);
                }
            };
            animation.setInterpolator(INTERPOLATOR);
            animation.setDuration(duration);
            startAnimation(animation);
        }

        // Now animate the indicator
        mTabStrip.animateIndicatorToPosition(newPosition, duration);
    }

    private void setSelectedTabView(int position) {
        final int tabCount = mTabStrip.getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = mTabStrip.getChildAt(i);
            final boolean isSelected = i == position;
            child.setSelected(isSelected);
        }
    }

    private void updateSelectedTabView(int position) {
        final TabView child = (TabView) mTabStrip.getChildAt(position);
        child.updateSelected(true);
    }

    private static boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    void selectTab(Tab tab) {
        if (mSelectedTab == tab) {
            if (mSelectedTab != null) {
                if (mOnTabSelectedListener != null) {
                    mOnTabSelectedListener.onTabReselected(mSelectedTab);
                }
                animateToTab(tab.getPosition());
            }
        } else {
            final int newPosition = tab != null ? tab.getPosition() : Tab.INVALID_POSITION;
            setSelectedTabView(newPosition);

            if ((mSelectedTab == null || mSelectedTab.getPosition() == Tab.INVALID_POSITION)
                    && newPosition != Tab.INVALID_POSITION) {
                // If we don't currently have a tab, just draw the indicator
                setScrollPosition(newPosition, 0f);
            } else {
                animateToTab(newPosition);
            }

            if (mSelectedTab != null && mOnTabSelectedListener != null) {
                mOnTabSelectedListener.onTabUnselected(mSelectedTab);
            }
            mSelectedTab = tab;
            if (mSelectedTab != null && mOnTabSelectedListener != null) {
                mOnTabSelectedListener.onTabSelected(mSelectedTab);
            }
        }
    }

    private int calculateScrollXForTab(int position, float positionOffset) {
        if (mMode == MODE_SCROLLABLE) {
            final View selectedChild = mTabStrip.getChildAt(position);
            final View nextChild = position + 1 < mTabStrip.getChildCount()
                    ? mTabStrip.getChildAt(position + 1)
                    : null;
            final int selectedWidth = selectedChild != null ? selectedChild.getWidth() : 0;
            final int nextWidth = nextChild != null ? nextChild.getWidth() : 0;

            return (int) (selectedChild.getLeft()
                    + ((selectedWidth + nextWidth) * positionOffset * 0.5f)
                    + selectedChild.getWidth() * 0.5f
                    - getWidth() * 0.5f);
        }
        return 0;
    }

    private void applyModeAndGravity() {
        int paddingStart = 0;
        if (mMode == MODE_SCROLLABLE) {
            // If we're scrollable, or fixed at start, inset using padding
            paddingStart = Math.max(0, mContentInsetStart - mTabPaddingStart);
        }
        ViewCompat.setPaddingRelative(mTabStrip, paddingStart, 0, 0, 0);

        switch (mMode) {
            case MODE_FIXED:
                mTabStrip.setGravity(Gravity.CENTER_HORIZONTAL);
                break;
            case MODE_SCROLLABLE:
                mTabStrip.setGravity(GravityCompat.START);
                break;
        }

        updateTabViewsLayoutParams();
    }

    private void updateTabViewsLayoutParams() {
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View child = mTabStrip.getChildAt(i);
            updateTabViewLayoutParams((LinearLayout.LayoutParams) child.getLayoutParams());
            child.requestLayout();
        }
    }

    private void updateTabViewsOrientation() {
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) mTabStrip.getChildAt(i);
            child.setOrientation(mTabOrientation);
            child.requestLayout();
        }
    }

    /**
     * A tab in this layout. Instances can be created via {@link #newTab()}.
     */
    public static final class Tab {

        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        private Object mTag;
        private Drawable mIcon;
        private CharSequence mText;
        private CharSequence mContentDesc;
        private int mPosition = INVALID_POSITION;
        private View mCustomView;

        private final TabLayout mParent;

        Tab(TabLayout parent) {
            mParent = parent;
        }

        /**
         * @return This Tab's tag object.
         */
        public Object getTag() {
            return mTag;
        }

        /**
         * Give this Tab an arbitrary object to hold for later use.
         *
         * @param tag Object to store
         * @return The current instance for call chaining
         */
        public Tab setTag(Object tag) {
            mTag = tag;
            return this;
        }

        View getCustomView() {
            return mCustomView;
        }

        /**
         * Set a custom view to be used for this tab. This overrides values set by {@link
         * #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param view Custom view to be used as a tab.
         * @return The current instance for call chaining
         */
        public Tab setCustomView(View view) {
            mCustomView = view;
            if (mPosition >= 0) {
                mParent.updateTab(mPosition);
            }
            return this;
        }

        /**
         * Set a custom view to be used for this tab. This overrides values set by {@link
         * #setText(CharSequence)} and {@link #setIcon(Drawable)}.
         *
         * @param layoutResId A layout resource to inflate and use as a custom tab view
         * @return The current instance for call chaining
         */
        public Tab setCustomView(int layoutResId) {
            return setCustomView(
                    LayoutInflater.from(mParent.getContext()).inflate(layoutResId, null));
        }

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        public Drawable getIcon() {
            return mIcon;
        }

        /**
         * Return the current position of this tab in the action bar.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
         * the action bar.
         */
        public int getPosition() {
            return mPosition;
        }

        void setPosition(int position) {
            mPosition = position;
        }

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        public CharSequence getText() {
            return mText;
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         * @return The current instance for call chaining
         */
        public Tab setIcon(Drawable icon) {
            mIcon = icon;
            if (mPosition >= 0) {
                mParent.updateTab(mPosition);
            }
            return this;
        }

        /**
         * Set the icon displayed on this tab.
         *
         * @param resId A resource ID referring to the icon that should be displayed
         * @return The current instance for call chaining
         */
        public Tab setIcon(int resId) {
            return setIcon(AppCompatDrawableManager.get().getDrawable(mParent.getContext(), resId));
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        public Tab setText(CharSequence text) {
            mText = text;
            if (mPosition >= 0) {
                mParent.updateTab(mPosition);
            }
            return this;
        }

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param resId A resource ID referring to the text that should be displayed
         * @return The current instance for call chaining
         */
        public Tab setText(int resId) {
            return setText(mParent.getResources().getText(resId));
        }

        /**
         * Set selected status for this tab. Specific method to fix status for initial element.
         */
        public void updateSelected() {
            mParent.updateSelectedTabView(this.getPosition());
        }

        /**
         * Select this tab. Only valid if the tab has been added to the action bar.
         */
        public void select() {
            mParent.selectTab(this);
        }

        /**
         * Set a description of this tab's content for use in accessibility support. If no content
         * description is provided the title will be used.
         *
         * @param resId A resource ID referring to the description text
         * @return The current instance for call chaining
         * @see #setContentDescription(CharSequence)
         * @see #getContentDescription()
         */
        public Tab setContentDescription(int resId) {
            return setContentDescription(mParent.getResources().getText(resId));
        }

        /**
         * Set a description of this tab's content for use in accessibility support. If no content
         * description is provided the title will be used.
         *
         * @param contentDesc Description of this tab's content
         * @return The current instance for call chaining
         * @see #setContentDescription(int)
         * @see #getContentDescription()
         */
        public Tab setContentDescription(CharSequence contentDesc) {
            mContentDesc = contentDesc;
            if (mPosition >= 0) {
                mParent.updateTab(mPosition);
            }
            return this;
        }

        /**
         * Gets a brief description of this tab's content for use in accessibility support.
         *
         * @return Description of this tab's content
         * @see #setContentDescription(CharSequence)
         * @see #setContentDescription(int)
         */
        public CharSequence getContentDescription() {
            return mContentDesc;
        }
    }

    class TabView extends LinearLayout implements OnLongClickListener {

        private final Tab mTab;
        private TextView mTextView;
        private ImageView mIconView;
        private View mCustomView;

        public TabView(Context context, Tab tab) {
            super(context);
            mTab = tab;
            if (mTabBackgroundResId != 0) {
                setBackgroundDrawable(AppCompatDrawableManager.get().getDrawable(context,
                        mTabBackgroundResId));
            }
            ViewCompat.setPaddingRelative(this, mTabPaddingStart, mTabPaddingTop,
                    mTabPaddingEnd, mTabPaddingBottom);
            setGravity(Gravity.CENTER);
            setOrientation(mTabOrientation);
            update();
        }

        @Override
        public void setSelected(boolean selected) {
            final boolean changed = (isSelected() != selected);
            super.setSelected(selected);
            if (changed && selected) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);

                if (mTextView != null) {
                    mTextView.setSelected(selected);
                }
                if (mIconView != null) {
                    mIconView.setSelected(selected);
                }
            }
        }

        public void updateSelected(boolean selected) {
            if (mTextView != null) {
                mTextView.setSelected(selected);
            }
            if (mIconView != null) {
                mIconView.setSelected(selected);
            }
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (getMeasuredWidth() > mTabMaxWidth) {
                // Re-measure if we went beyond our maximum size.
                super.onMeasure(MeasureSpec.makeMeasureSpec(
                        mTabMaxWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            } else if (mTabMinWidth > 0 && getMeasuredHeight() < mTabMinWidth) {
                // Re-measure if we're below our minimum size.
                super.onMeasure(MeasureSpec.makeMeasureSpec(
                        mTabMinWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            }
        }

        final void update() {
            final Tab tab = mTab;
            final View custom = tab.getCustomView();
            if (custom != null) {
                final ViewParent customParent = custom.getParent();
                if (customParent != this) {
                    if (customParent != null) {
                        ((ViewGroup) customParent).removeView(custom);
                    }
                    addView(custom);
                }
                mCustomView = custom;
                if (mTextView != null) {
                    mTextView.setVisibility(GONE);
                }
                if (mIconView != null) {
                    mIconView.setVisibility(GONE);
                    mIconView.setImageDrawable(null);
                }
            } else {
                if (mCustomView != null) {
                    removeView(mCustomView);
                    mCustomView = null;
                }

                final Drawable icon = tab.getIcon();
                final CharSequence text = tab.getText();

                if (icon != null) {
                    if (mIconView == null) {
                        ImageView iconView = new ImageView(getContext());
                        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT);
                        lp.gravity = Gravity.CENTER;
                        iconView.setLayoutParams(lp);

                        if (mTabOrientation == HORIZONTAL) {
                            ViewCompat.setPaddingRelative(iconView, 0, 0,
                                    mTabPaddingInternal, 0);
                        } else {
                            ViewCompat.setPaddingRelative(iconView, 0, 0,
                                    0, mTabPaddingInternal);
                        }

                        addView(iconView, 0);
                        mIconView = iconView;
                    }
                    mIconView.setImageDrawable(icon);
                    mIconView.setVisibility(VISIBLE);
                } else if (mIconView != null) {
                    mIconView.setVisibility(GONE);
                    mIconView.setImageDrawable(null);
                }

                final boolean hasText = !TextUtils.isEmpty(text);
                if (hasText) {
                    if (mTextView == null) {
                        TextView textView = new TextView(getContext());
                        textView.setMaxLines(MAX_TAB_TEXT_LINES);
                        textView.setEllipsize(TextUtils.TruncateAt.END);
                        textView.setGravity(Gravity.CENTER);

                        addView(textView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        mTextView = textView;
                    }

                    mTextView.setTextAppearance(getContext(), mTabTextAppearance);
                    if (mTabSelectedTextColorSet) {
                        mTextView.setTextColor(createColorStateList(
                                mTextView.getCurrentTextColor(), mTabSelectedTextColor));
                    }

                    mTextView.setText(text);
                    mTextView.setVisibility(VISIBLE);
                } else if (mTextView != null) {
                    mTextView.setTextAppearance(getContext(), mTabTextAppearance);
                    if (mTabSelectedTextColorSet) {
                        mTextView.setTextColor(createColorStateList(
                                mTextView.getCurrentTextColor(), mTabSelectedTextColor));
                    }

                    mTextView.setVisibility(GONE);
                    mTextView.setText(null);
                }

                if (mIconView != null) {
                    mIconView.setContentDescription(tab.getContentDescription());
                }

                if (!hasText && !TextUtils.isEmpty(tab.getContentDescription())) {
                    setOnLongClickListener(this);
                } else {
                    setOnLongClickListener(null);
                    setLongClickable(false);
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            final int[] screenPos = new int[2];
            getLocationOnScreen(screenPos);

            final Context context = getContext();
            final int width = getWidth();
            final int height = getHeight();
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

            Toast cheatSheet = Toast.makeText(context, mTab.getContentDescription(),
                    Toast.LENGTH_SHORT);
            // Show under the tab
            cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                    (screenPos[0] + width / 2) - screenWidth / 2, height);

            cheatSheet.show();
            return true;
        }

        private ColorStateList createColorStateList(int defaultColor, int selectedColor) {
            final int[][] states = new int[2][];
            final int[] colors = new int[2];
            int i = 0;

            states[i] = SELECTED_STATE_SET;
            colors[i] = selectedColor;
            i++;

            // Default enabled state
            states[i] = EMPTY_STATE_SET;
            colors[i] = defaultColor;
            i++;

            return new ColorStateList(states, colors);
        }

        public Tab getTab() {
            return mTab;
        }
    }

    private class SlidingTabStrip extends LinearLayout {

        /**
         * Gravity used to place tab indicator to the bottom of tab
         *
         * @see #setSelectedIndicatorGravity(int)
         */
        public static final int GRAVITY_BOTTOM = 0;

        /**
         * Gravity used to place tab indicator on top of tab
         *
         * @see #setSelectedIndicatorGravity(int)
         */
        public static final int GRAVITY_TOP = 1;

        private int mSelectedIndicatorGravity;
        private int mSelectedIndicatorHeight;
        private final Paint mSelectedIndicatorPaint;

        private int mSelectedPosition = -1;
        private float mSelectionOffset;

        private int mIndicatorLeft = -1;
        private int mIndicatorRight = -1;

        private boolean mIsIndicatorScrollable = true;
        private boolean mHasInit = false;

        SlidingTabStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            mSelectedIndicatorPaint = new Paint();
        }

        void setSelectedIndicatorGravity(int gravity) {
            mSelectedIndicatorGravity = gravity;
            ViewCompat.postInvalidateOnAnimation(this);
        }

        void setSelectedIndicatorColor(int color) {
            mSelectedIndicatorPaint.setColor(color);
            ViewCompat.postInvalidateOnAnimation(this);
        }

        void setSelectedIndicatorHeight(int height) {
            mSelectedIndicatorHeight = height;
            ViewCompat.postInvalidateOnAnimation(this);
        }

        void setIndicatorPositionFromTabPosition(int position, float positionOffset) {
            if (!mIsIndicatorScrollable && mHasInit) return;

            if (isAnimationRunning(getAnimation())) {
                return;
            }
            mSelectedPosition = position;
            mSelectionOffset = positionOffset;
            updateIndicatorPosition();
            mHasInit = true;
        }

        void setSelectedIndicatorScrollable(boolean isScrollable) {
            mIsIndicatorScrollable = isScrollable;
        }

        @Override
        protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                // HorizontalScrollView will first measure use with UNSPECIFIED, and then with
                // EXACTLY. Ignore the first call since anything we do will be overwritten anyway
                return;
            }

            if (mMode == MODE_FIXED && mTabGravity == GRAVITY_CENTER) {
                final int count = getChildCount();

                final int unspecifiedSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

                // First we'll find the largest tab
                int largestTabWidth = 0;
                for (int i = 0, z = count; i < z; i++) {
                    final View child = getChildAt(i);
                    child.measure(unspecifiedSpec, heightMeasureSpec);
                    largestTabWidth = Math.max(largestTabWidth, child.getMeasuredWidth());
                }

                if (largestTabWidth <= 0) {
                    // If we don't have a largest child yet, skip until the next measure pass
                    return;
                }

                final int gutter = dpToPx(FIXED_WRAP_GUTTER_MIN);
                if (largestTabWidth * count <= getMeasuredWidth() - gutter * 2) {
                    // If the tabs fit within our width minus gutters, we will set all tabs to have
                    // the same width
                    for (int i = 0; i < count; i++) {
                        final View child = getChildAt(i);
                        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        lp.width = largestTabWidth;
                        lp.weight = 0;
                    }
                } else {
                    // If the tabs will wrap to be larger than the width minus gutters, we need
                    // to switch to GRAVITY_FILL
                    mTabGravity = GRAVITY_FILL;
                    updateTabViewsLayoutParams();
                }

                // Now re-measure after our changes
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);

            if (!isAnimationRunning(getAnimation())) {
                // If we've been layed out, and we're not currently in an animation, update the
                // indicator position
                updateIndicatorPosition();
            }
        }

        private void updateIndicatorPosition() {
            final View selectedTitle = getChildAt(mSelectedPosition);
            int left, right;

            if (selectedTitle != null && selectedTitle.getWidth() > 0) {
                left = selectedTitle.getLeft();
                right = selectedTitle.getRight();

                if (mSelectionOffset > 0f && mSelectedPosition < getChildCount() - 1) {
                    // Draw the selection partway between the tabs
                    View nextTitle = getChildAt(mSelectedPosition + 1);
                    left = (int) (mSelectionOffset * nextTitle.getLeft() +
                            (1.0f - mSelectionOffset) * left);
                    right = (int) (mSelectionOffset * nextTitle.getRight() +
                            (1.0f - mSelectionOffset) * right);
                }
            } else {
                left = right = -1;
            }

            setIndicatorPosition(left, right);
        }

        private void setIndicatorPosition(int left, int right) {
            if (left != mIndicatorLeft || right != mIndicatorRight) {
                // If the indicator's left/right has changed, invalidate
                mIndicatorLeft = left;
                mIndicatorRight = right;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        void animateIndicatorToPosition(final int position, int duration) {
            final boolean isRtl = ViewCompat.getLayoutDirection(this)
                    == ViewCompat.LAYOUT_DIRECTION_RTL;

            final View targetView = getChildAt(position);
            final int targetLeft = targetView.getLeft();
            final int targetRight = targetView.getRight();
            final int startLeft;
            final int startRight;

            if (Math.abs(position - mSelectedPosition) <= 1) {
                // If the views are adjacent, we'll animate from edge-to-edge
                startLeft = mIndicatorLeft;
                startRight = mIndicatorRight;
            } else {
                // Else, we'll just grow from the nearest edge
                final int offset = dpToPx(MOTION_NON_ADJACENT_OFFSET);
                if (position < mSelectedPosition) {
                    // We're going end-to-start
                    if (isRtl) {
                        startLeft = startRight = targetLeft - offset;
                    } else {
                        startLeft = startRight = targetRight + offset;
                    }
                } else {
                    // We're going start-to-end
                    if (isRtl) {
                        startLeft = startRight = targetRight + offset;
                    } else {
                        startLeft = startRight = targetLeft - offset;
                    }
                }
            }

            if (startLeft != targetLeft || startRight != targetRight) {
                final Animation anim = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        setIndicatorPosition(
                                (int) lerp(startLeft, targetLeft, interpolatedTime),
                                (int) lerp(startRight, targetRight, interpolatedTime));
                    }
                };
                anim.setInterpolator(INTERPOLATOR);
                anim.setDuration(duration);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (mOnTabAnimationListener != null) {
                            mOnTabAnimationListener.onTabAnimationStart(getTabAt(mSelectedPosition));
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mSelectedPosition = position;
                        mSelectionOffset = 0f;

                        if (mOnTabAnimationListener != null) {
                            mOnTabAnimationListener.onTabAnimationEnd(getTabAt(position));
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                startAnimation(anim);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            if (mIndicatorLeft >= 0 && mIndicatorRight > mIndicatorLeft) {
                if (mSelectedIndicatorGravity == GRAVITY_BOTTOM) {
                    canvas.drawRect(mIndicatorLeft, (float) (getHeight() - mSelectedIndicatorHeight),
                            mIndicatorRight, getHeight(), mSelectedIndicatorPaint);
                } else {
                    canvas.drawRect(mIndicatorLeft, 0.f, mIndicatorRight,
                            (float) mSelectedIndicatorHeight, mSelectedIndicatorPaint);
                }
            }
        }
    }

    /**
     * Linear interpolation between {@code startValue} and {@code endValue} by the fraction {@code
     * fraction}.
     */
    static float lerp(float startValue, float endValue, float fraction) {
        return startValue + (fraction * (endValue - startValue));
    }
}
