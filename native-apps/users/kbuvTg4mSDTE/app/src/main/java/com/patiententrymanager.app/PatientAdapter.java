package com.patiententrymanager.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends BaseAdapter implements Filterable {

    private final Context context;
    private List<Patient> originalList;
    private List<Patient> filteredList;
    private final LayoutInflater inflater;
    private PatientFilter filter;

    public PatientAdapter(Context context, List<Patient> patients) {
        this.context = context;
        this.originalList = patients != null ? patients : new ArrayList<Patient>();
        this.filteredList = new ArrayList<Patient>(this.originalList);
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<Patient> newPatients) {
        this.originalList = newPatients != null ? newPatients : new ArrayList<Patient>();
        this.filteredList = new ArrayList<Patient>(this.originalList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Patient getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_patient, parent, false);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.tvPatientName);
            holder.tvMeta = (TextView) convertView.findViewById(R.id.tvPatientMeta);
            holder.tvVisitDate = (TextView) convertView.findViewById(R.id.tvVisitDate);
            holder.tvHasPhoto = (TextView) convertView.findViewById(R.id.tvHasPhoto);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Patient p = getItem(position);
        holder.tvName.setText(p.getName());
        holder.tvMeta.setText("Age: " + p.getAge() + " | Gender: " + p.getGender() + " | Phone: " + p.getPhone());
        holder.tvVisitDate.setText("Visit: " + p.getVisitDate());

        if (p.getImageId() != null && !p.getImageId().trim().isEmpty()) {
            holder.tvHasPhoto.setVisibility(View.VISIBLE);
        } else {
            holder.tvHasPhoto.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new PatientFilter();
        }
        return filter;
    }

    static class ViewHolder {
        TextView tvName;
        TextView tvMeta;
        TextView tvVisitDate;
        TextView tvHasPhoto;
    }

    private class PatientFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.toString().trim().isEmpty()) {
                results.values = originalList;
                results.count = originalList.size();
            } else {
                String query = constraint.toString().toLowerCase().trim();
                List<Patient> matches = new ArrayList<Patient>();
                for (Patient p : originalList) {
                    if (p.getName().toLowerCase().contains(query)
                            || p.getPhone().toLowerCase().contains(query)
                            || p.getVisitDate().toLowerCase().contains(query)
                            || String.valueOf(p.getAge()).contains(query)) {
                        matches.add(p);
                    }
                }
                results.values = matches;
                results.count = matches.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList = (List<Patient>) results.values;
            notifyDataSetChanged();
        }
    }
}