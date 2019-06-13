package com.dyd.libsource.dropdownbox;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.dyd.libsource.R;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * author : dyd
 * time   : 2019/06/13
 * desc   : xxxx 描述
 */
public class MySpinner extends AppCompatTextView {
    private Context context;
    private int popBgColor;
    private int textColor;
    private int itemTextColor;
    private int arrowColor;
    private boolean isHideArrow;
    private int backgroundSelector;
    private int popwindowMaxHeight;
    private int popwindowHeight;
    private int arrowColorDisabled;


    private Drawable arrowDrawable; //箭头布局 视图
    private ListView listView; //布局
    private MySpinnerBaseAdapter adapter; //listView的适配
    private int selectedIndex;//选择的条目下标
    private boolean nothingSelected;//没有选择任何条目
    private OnItemSelectedListener onItemSelectedListener;
    private OnNothingSelectedListener onNothingSelectedListener;
    private PopupWindow popupWindow; //使用popupWindow控件 样式

    public MySpinner(Context context) {
        super(context);
        this.context=context;
        init(context,null);
    }

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context=context;
        init(context,attrs);
    }

    public MySpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context=context;
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        //获取attrs.xml中设置的参数
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MySpinner);
        //默认色设置
        int defaultColor = getTextColors().getDefaultColor();

        boolean rtl = Utils.isRtl(context);
        try {
            //获取背景色（条目内容popwindow背景色）
            popBgColor = typedArray.getColor(R.styleable.MySpinner_sp_background_color, Color.WHITE);
            textColor = typedArray.getColor(R.styleable.MySpinner_sp_text_color, defaultColor);
            itemTextColor = typedArray.getColor(R.styleable.MySpinner_sp_item_text_color, textColor);
            arrowColor = typedArray.getColor(R.styleable.MySpinner_sp_arrow_color, textColor);
            isHideArrow = typedArray.getBoolean(R.styleable.MySpinner_sp_hide_arrow, false);
            backgroundSelector = typedArray.getColor(R.styleable.MySpinner_sp_background_selector, 0);
            popwindowMaxHeight = typedArray.getDimensionPixelSize(R.styleable.MySpinner_sp_popupwindow_maxheight, 0);
            popwindowHeight = typedArray.getLayoutDimension(R.styleable.MySpinner_sp_popupwindow_height, WindowManager.LayoutParams.WRAP_CONTENT);
            arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        } finally {
            typedArray.recycle();
        }

        Resources resources = getResources();
        int left = resources.getDimensionPixelSize(R.dimen.sp_padding_left);
        int right = resources.getDimensionPixelSize(R.dimen.sp_padding_right);
        if (rtl) {
            right = resources.getDimensionPixelSize(R.dimen.sp_padding_left);
        } else {
            left = resources.getDimensionPixelSize(R.dimen.sp_padding_right);
        }

        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);//设置选中内容垂直居中向左
        setClickable(true);//设置可点击
        setPadding(left, 0, right, 0);

        //设置 在界面中的显示样式，以及点击样式，区分按压和不按压控件情况
        setBackgroundResource(R.drawable.sp_selector_style);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && rtl) {
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            setTextDirection(View.TEXT_DIRECTION_RTL);
        }

        /**设置箭头布局
         *
         */
        if (!isHideArrow) {
            arrowDrawable = ContextCompat.getDrawable(context, R.drawable.sp_arrow).mutate();
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
            if (rtl) {
                setCompoundDrawablesWithIntrinsicBounds(arrowDrawable, null, null, null);
            } else {
                setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null);
            }
        }

        //创建布局 使用listView控件展示items
        listView = new ListView(context);
        listView.setId(getId());
        listView.setDivider(null);
        listView.setItemsCanFocus(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= selectedIndex && position < adapter.getCount()) {
                    position++;
                }
                selectedIndex = position;
                nothingSelected = false;
                Object item = adapter.get(position);
                adapter.notifyItemSelected(position);
                setText(item.toString());
                collapse();
                if (onItemSelectedListener != null) {
                    //noinspection unchecked
                    onItemSelectedListener.onItemSelected(MySpinner.this, position, id, item);
                }
            }
        });

        //使用PopupWindow控件承载数据
        popupWindow = new PopupWindow(context);
        popupWindow.setContentView(listView);//
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        //设置背景色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(16);//设置阴影
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.sp_popwindow_bg));// R.drawable.ms__drawable
        } else {
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.sp_popwindow_bg));
        }

        //设置背景
        if (popBgColor != Color.WHITE) { // default color is white
            setBackgroundColor(popBgColor);
        } else if (backgroundSelector != 0) {
            //改变最底层颜色
            setBackgroundResource(backgroundSelector);
        }
        //数据显示颜色
        if (textColor != defaultColor) {
            setTextColor(textColor);
        }

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                if (nothingSelected && onNothingSelectedListener != null) {
                    onNothingSelectedListener.onNothingSelected(MySpinner.this);
                }
                if (!isHideArrow) {
                    animateArrow(false);
                }
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        popupWindow.setWidth(MeasureSpec.getSize(widthMeasureSpec));
        popupWindow.setHeight(calculatePopupWindowHeight());
        if (adapter != null) {
            CharSequence currentText = getText();
            String longestItem = currentText.toString();
            for (int i = 0; i < adapter.getCount(); i++) {
                String itemText = adapter.getItemText(i);
                if (itemText.length() > longestItem.length()) {
                    longestItem = itemText;
                }
            }
            setText(longestItem);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setText(currentText);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isEnabled() && isClickable()) {
                if (!popupWindow.isShowing()) {
                    expand();
                } else {
                    collapse();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setBackgroundColor(int color) {
        popBgColor=color;
        Drawable background = getBackground();
        if (background instanceof StateListDrawable) { // pre-L
            try {
                Method getStateDrawable = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                if (!getStateDrawable.isAccessible())
                    getStateDrawable.setAccessible(true);
                int[] colors = {Utils.darker(color, 0.85f), color};
                for (int i = 0; i < colors.length; i++) {
                    ColorDrawable drawable = (ColorDrawable) getStateDrawable.invoke(background, i);
                    drawable.setColor(colors[i]);
                }
            } catch (Exception e) {
                Log.e("MaterialSpinner", "Error setting background color", e);
            }
        } else if (background != null) { // 21+ (RippleDrawable)
            background.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
        popupWindow.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void setTextColor(int color) {
        textColor = color;
        super.setTextColor(color);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("state", super.onSaveInstanceState());
        bundle.putInt("selected_index", selectedIndex);
        if (popupWindow != null) {
            bundle.putBoolean("is_popup_showing", popupWindow.isShowing());
            collapse();
        } else {
            bundle.putBoolean("is_popup_showing", false);
        }
        return bundle;
    }

    /**
     * 获取状态
     *
     * @param savedState
     */
    @Override
    public void onRestoreInstanceState(Parcelable savedState) {
        if (savedState instanceof Bundle) {
            Bundle bundle = (Bundle) savedState;
            selectedIndex = bundle.getInt("selected_index");
            if (adapter != null) {
                setText(adapter.get(selectedIndex).toString());
                adapter.notifyItemSelected(selectedIndex);
            }
            if (bundle.getBoolean("is_popup_showing")) {
                if (popupWindow != null) {
                    // Post the show request into the looper to avoid bad token exception
                    post(new Runnable() {

                        @Override
                        public void run() {
                            expand();
                        }
                    });
                }
            }
            savedState = bundle.getParcelable("state");
        }
        super.onRestoreInstanceState(savedState);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(enabled ? arrowColor : arrowColorDisabled, PorterDuff.Mode.SRC_IN);
        }
    }
    /**
     * @return the selected item position
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Set the default spinner item using its index
     * 初始界面 常用
     *
     * @param position the item's position
     */
    public void setSelectedIndex(int position) {
        if (adapter != null) {
            if (position >= 0 && position <= adapter.getCount()) {
                adapter.notifyItemSelected(position);
                selectedIndex = position;
                setText(adapter.get(position).toString());
            } else {
                throw new IllegalArgumentException("Position must be lower than adapter count!");
            }
        }
    }

    /**
     * Set the dropdown items
     *
     * @param items A list of items
     * @param <T>   The item type
     */
    public <T> void setItems(@NonNull T... items) {
        setItems(Arrays.asList(items));
    }

    /**
     * Set the dropdown items
     *
     * @param items A list of items
     * @param <T>   The item type
     */
    public <T> void setItems(@NonNull List<T> items) {
        adapter = new MySpinnerAdapter<>(getContext(), items).setBackgroundSelector(backgroundSelector).setTextColor(itemTextColor);
        setAdapterInternal(adapter);
    }

    public <T> List<T> getItems() {
        if (adapter == null) {
            return null;
        }
        //noinspection unchecked
        return adapter.getItems();
    }

    public void setAdapter(@NonNull ListAdapter adapter) {
        this.adapter = new MySpinnerAdapterWrapper(getContext(), adapter).setBackgroundSelector(backgroundSelector)
                .setTextColor(itemTextColor);
        setAdapterInternal(this.adapter);
    }

    public <T> void setAdapter(MySpinnerAdapter<T> adapter) {
        this.adapter = adapter;
        this.adapter.setTextColor(itemTextColor);
        this.adapter.setBackgroundSelector(backgroundSelector);
        setAdapterInternal(adapter);
    }

    /**
     * 数据绑定+显示
     *
     * @param adapter
     */
    private void setAdapterInternal(@NonNull MySpinnerBaseAdapter adapter) {
        listView.setAdapter(adapter);
        if (selectedIndex >= adapter.getCount()) {
            selectedIndex = 0;
        }
        if (adapter.getCount() > 0) {
            setText(adapter.get(selectedIndex).toString());
        } else {
            setText("");
        }
    }

    /**
     * 收起
     */
    public void collapse() {
        if (!isHideArrow) {
            animateArrow(false);
        }
        popupWindow.dismiss();
    }

    /**
     * 展开
     */
    public void expand() {
        if (!isHideArrow) {
            animateArrow(true);
        }
        nothingSelected = true;
        popupWindow.showAsDropDown(this);
    }


    private void animateArrow(boolean shouldRotateUp) {
        int start = shouldRotateUp ? 0 : 10000;
        int end = shouldRotateUp ? 10000 : 0;
        ObjectAnimator animator = ObjectAnimator.ofInt(arrowDrawable, "level", start, end);
        animator.start();
    }

    public void setArrowColor(@ColorInt int color) {
        arrowColor = color;
        arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
        }
    }

    public PopupWindow getPopupWindow() {
        return popupWindow;
    }

    public ListView getListView() {
        return listView;
    }

    /**
     * 计算popupWindow控件 弹窗的高度
     *
     * @return
     */
    private int calculatePopupWindowHeight() {
        if (adapter == null) {
            return WindowManager.LayoutParams.WRAP_CONTENT;
        }
        //计算出listView的总高度
        float listViewHeight = adapter.getCount() * getResources().getDimension(R.dimen.sp_item_height);

        //如果xml布局中设置了最高高度，且listViewHeight高度满足，优先使用最高高度
        if (popwindowMaxHeight > 0 && listViewHeight > popwindowMaxHeight) {

            return popwindowMaxHeight;

        } else if (popwindowHeight != WindowManager.LayoutParams.MATCH_PARENT
                && popwindowHeight != WindowManager.LayoutParams.WRAP_CONTENT
                && popwindowHeight <= listViewHeight) {
            return popwindowHeight;
        }
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public interface OnItemSelectedListener<T> {

        /**
         * <p>Callback method to be invoked when an item in this view has been selected. This callback is invoked only when
         * the newly selected position is different from the previously selected position or if there was no selected
         * item.</p>
         *
         * @param view     The {@link MySpinner} view
         * @param position The position of the view in the adapter
         * @param id       The row id of the item that is selected
         * @param item     The selected item
         */
        void onItemSelected(MySpinner view, int position, long id, T item);
    }


    public void setOnNothingSelectedListener(@Nullable OnNothingSelectedListener onNothingSelectedListener) {
        this.onNothingSelectedListener = onNothingSelectedListener;
    }
    /**
     * Interface definition for a callback to be invoked when the dropdown is dismissed and no item was selected.
     */
    public interface OnNothingSelectedListener {

        /**
         * Callback method to be invoked when the {@link PopupWindow} is dismissed and no item was selected.
         *
         * @param spinner the {@link MySpinner}
         */
        void onNothingSelected(MySpinner spinner);
    }
}
