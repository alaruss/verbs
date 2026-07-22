package com.alaruss.verbs.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.alaruss.verbs.R;
import com.alaruss.verbs.models.Verb;

import java.util.ArrayList;
import java.util.List;

public class VerbRecyclerAdapter extends RecyclerView.Adapter<VerbRecyclerAdapter.VerbViewHolder> {
    private List<Verb> verbs;
    private OnVerbClickListener listener;

    public VerbRecyclerAdapter(OnVerbClickListener listener) {
        this.verbs = new ArrayList<>();
        this.listener = listener;
    }

    public List<Verb> getVerbs() {
        return verbs;
    }

    public void setVerbs(List<Verb> newVerbs) {
        if (newVerbs == null) {
            newVerbs = new ArrayList<>();
        }

        final List<Verb> oldVerbs = this.verbs;
        final List<Verb> finalNewVerbs = newVerbs;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldVerbs.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewVerbs.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldVerbs.get(oldItemPosition).getId() == finalNewVerbs.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Verb oldVerb = oldVerbs.get(oldItemPosition);
                Verb newVerb = finalNewVerbs.get(newItemPosition);
                return oldVerb.getInfinitive().equals(newVerb.getInfinitive()) &&
                        oldVerb.isFavorite() == newVerb.isFavorite();
            }
        });

        this.verbs = newVerbs;
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VerbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.verb_list_item, parent, false);
        return new VerbViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerbViewHolder holder, int position) {
        Verb verb = verbs.get(position);
        holder.bind(verb, position);
    }

    @Override
    public int getItemCount() {
        return verbs.size();
    }

    public interface OnVerbClickListener {
        void onVerbClick(Verb verb, int position);
    }

    class VerbViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;

        public VerbViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
        }

        public void bind(final Verb verb, final int position) {
            titleTextView.setText(verb.getInfinitive());
            itemView.setOnClickListener(v -> {
                // Resolve the current position at click time. DiffUtil may shift a
                // surviving row without re-binding it, so a captured position can go
                // stale (e.g. when the list narrows to a single result).
                int pos = getBindingAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onVerbClick(verbs.get(pos), pos);
                }
            });
        }
    }
}
