package in.srain.cube.views.ptr;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PtrClassicFrameLayout extends PtrFrameLayout {

    private XListViewHeader mHeaderView;
    private XListViewFooter mFooterView;
    private RelativeLayout mHeaderViewContent;
    private TextView mHeaderTimeView;

    public PtrClassicFrameLayout(Context context) {
        super(context);
        initViews();
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    private void initViews() {
        mHeaderView = new XListViewHeader(getContext());
        mHeaderViewContent = (RelativeLayout) mHeaderView
                .findViewById(R.id.xlistview_header_content);
        mHeaderTimeView = (TextView) mHeaderView
                .findViewById(R.id.xlistview_header_time);
        setHeaderView(mHeaderView);
        addPtrUIHandler(mHeaderView);
        mFooterView = new XListViewFooter(getContext());
        setFooterView(mFooterView);
        addPtrUIHandler(mFooterView);
    }

    public XListViewHeader getHeader() {
        return mHeaderView;
    }

    public void setRefreshTime(String time) {
        mHeaderTimeView.setText(time);
    }

    /**
     * 是否显示加载更多的底部线条
     *
     * @param enable
     */
    public void setFooterLineEnable(boolean enable) {
        if (!enable) {
            mFooterView.hideLine();
        } else {
            mFooterView.showLine();
        }
    }


    public void setHeaderLineEnable(boolean enable) {
        if (!enable) {
            mHeaderView.hideLine();
        } else {
            mHeaderView.showLine();
        }
    }

    public void unMarginLine() {
        mHeaderView.unMarginLine();
        mFooterView.unMarginLine();
    }
}
