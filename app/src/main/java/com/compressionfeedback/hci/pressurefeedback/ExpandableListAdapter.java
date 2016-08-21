package com.compressionfeedback.hci.pressurefeedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> _listDataHeader;
    private HashMap<String, List<ApplicationInfo>> _listDataChild;
    private ArrayList<View> groupViews;

    public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                 HashMap<String, List<ApplicationInfo>> listChildData) {
        this.context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
        groupViews=new ArrayList<>();
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {


        ApplicationInfo data = (ApplicationInfo) getChild(groupPosition, childPosition);

        if (null == convertView) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.snippet_list_row, null);
        }

        if (null != data) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int pattern=preferences.getInt(data.packageName+"pattern",1);
            int strength =preferences.getInt(data.packageName+"strength",0);

            TextView appName = (TextView) convertView.findViewById(R.id.app_name);
            TextView patternChoice = (TextView) convertView.findViewById(R.id.pattern);
            TextView strengthChoice = (TextView) convertView.findViewById(R.id.strength);
            ImageView iconview = (ImageView) convertView.findViewById(R.id.app_icon);

            PackageManager packageManager=context.getPackageManager();
            appName.setText(data.loadLabel(packageManager));
            Resources resource = context.getResources();
            patternChoice.setText(resource.getStringArray(R.array.patterns_array)[pattern]);
            strengthChoice.setText(resource.getStringArray(R.array.strength_array)[strength]);
            iconview.setImageDrawable(data.loadIcon(packageManager));
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.expandable_list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);
        groupViews.add(convertView);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public ArrayList<View> getGroupViews(){
        return groupViews;
    }
}