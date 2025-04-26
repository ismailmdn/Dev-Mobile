package com.example.projetguermah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public static class Category {
        String name;
        int iconResId;
        String color;

        Category(String name, int iconResId, String color) {
            this.name = name;
            this.iconResId = iconResId;
            this.color = color;
        }
    }

    private final List<Category> categories;
    private final OnCategorySelectedListener listener;
    private int selectedPosition = 0;

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    public CategoryAdapter(OnCategorySelectedListener listener) {
        this.listener = listener;
        this.categories = getDefaultCategories();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        
        holder.nameText.setText(category.name);
        holder.iconImage.setImageResource(category.iconResId);
        
        // Set the selected state
        holder.itemView.setSelected(position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            
            // Update the UI
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            
            // Notify the listener
            listener.onCategorySelected(category.name);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImage;
        TextView nameText;

        CategoryViewHolder(View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.category_icon);
            nameText = itemView.findViewById(R.id.category_name);
        }
    }

    private List<Category> getDefaultCategories() {
        return new ArrayList<>(Arrays.asList(
            new Category("Food", android.R.drawable.ic_menu_manage, "#FFC107"),
            new Category("Social", android.R.drawable.ic_menu_share, "#FF9800"),
            new Category("Traffic", android.R.drawable.ic_menu_directions, "#2196F3"),
            new Category("Shopping", android.R.drawable.ic_menu_sort_by_size, "#E91E63"),
            new Category("Grocery", android.R.drawable.ic_menu_agenda, "#4CAF50"),
            new Category("Education", android.R.drawable.ic_menu_edit, "#9C27B0"),
            new Category("Bills", android.R.drawable.ic_menu_day, "#009688"),
            new Category("Rentals", android.R.drawable.ic_menu_compass, "#FFA000"),
            new Category("Medical", android.R.drawable.ic_menu_help, "#F44336"),
            new Category("Investment", android.R.drawable.ic_menu_info_details, "#00BCD4"),
            new Category("Gift", android.R.drawable.ic_menu_view, "#673AB7"),
            new Category("Other", android.R.drawable.ic_menu_more, "#03A9F4")
        ));
    }
} 