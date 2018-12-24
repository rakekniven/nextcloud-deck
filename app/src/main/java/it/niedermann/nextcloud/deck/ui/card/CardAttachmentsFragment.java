package it.niedermann.nextcloud.deck.ui.card;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import it.niedermann.nextcloud.deck.R;

public class CardAttachmentsFragment extends Fragment {
    private Unbinder unbinder;

    public static CardAttachmentsFragment newInstance() {
        CardAttachmentsFragment fragment = new CardAttachmentsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_card_edit_tab_attachments, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupView();

        return view;
    }

    private void setupView() {
        // TODO read/set available card attachments data
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
