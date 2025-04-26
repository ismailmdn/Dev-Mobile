package com.example.projetguermah;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onDeleteClick(Transaction transaction, int position);
    }

    public TransactionAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.titleTextView.setText(transaction.getTitle());
        
        if (transaction.getCategory() != null) {
            holder.categoryTextView.setText(transaction.getCategory());
            holder.categoryTextView.setVisibility(View.VISIBLE);
        } else {
            holder.categoryTextView.setVisibility(View.GONE);
        }
        
        // Format amount with + or - based on type
        String amountText = (transaction.getType().equals("income") ? "+" : "-") + 
                           String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
        holder.amountTextView.setText(amountText);
        
        // Set amount color based on type
        int color = holder.itemView.getContext().getResources().getColor(
                transaction.getType().equals("income") ? 
                android.R.color.holo_green_dark : 
                android.R.color.holo_red_dark);
        holder.amountTextView.setTextColor(color);

        if (transaction.getDate() != null) {
            holder.dateTextView.setText(dateFormat.format(transaction.getDate()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(transaction, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView categoryTextView;
        TextView amountTextView;
        TextView dateTextView;
        ImageButton deleteButton;

        TransactionViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.transaction_title);
            categoryTextView = itemView.findViewById(R.id.transaction_category);
            amountTextView = itemView.findViewById(R.id.transaction_amount);
            dateTextView = itemView.findViewById(R.id.transaction_date);
            deleteButton = itemView.findViewById(R.id.delete_transaction_btn);
        }
    }
} 