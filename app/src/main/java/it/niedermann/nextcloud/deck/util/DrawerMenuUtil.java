package it.niedermann.nextcloud.deck.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;
import java.util.Map;

import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.ui.board.AccessControlDialogFragment;
import it.niedermann.nextcloud.deck.ui.board.EditBoardDialogFragment;

public class DrawerMenuUtil {
    public static final int MENU_ID_ADD_ACCOUNT = -2;

    public static final int MENU_ID_ABOUT = -1;
    public static final int MENU_ID_ADD_BOARD = -2;
    public static final int MENU_ID_SETTINGS = -3;


    private DrawerMenuUtil() {

    }

    public static <T extends Context & DrawerAccountListener> void inflateAccounts(
            @NonNull T context,
            @NonNull Menu menu,
            @NonNull List<Account> accounts
    ) {
        int index = 0;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        final String sharedPreferencesLastBoardForAccount_ = context.getString(R.string.shared_preference_last_board_for_account_);
        final String sharedPreferencesLastStackForAccountAndBoard_ = context.getString(R.string.shared_preference_last_stack_for_account_and_board_);
        for (Account account : accounts) {
            final int currentIndex = index;
            MenuItem m = menu.add(Menu.NONE, index++, Menu.NONE, account.getName()).setIcon(R.drawable.ic_person_grey600_24dp);
            AppCompatImageButton contextMenu = new AppCompatImageButton(context);
            contextMenu.setBackgroundDrawable(null);

            String uri = account.getUrl() + "/index.php/avatar/" + Uri.encode(account.getUserName()) + "/56";
            Glide.with(context)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            m.setIcon(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

            contextMenu.setImageDrawable(ViewUtil.getTintedImageView(context, R.drawable.ic_delete_black_24dp, R.color.grey600));
            contextMenu.setOnClickListener((v) -> {
                if (currentIndex != 0) { // Select first account after deletion
                    context.onAccountChosen(accounts.get(0));
                } else if (accounts.size() > 1) { // Select second account after deletion
                    context.onAccountChosen(accounts.get(1));
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                Map<String, ?> allEntries = sharedPreferences.getAll();
                DeckLog.log("--- Remove: " + sharedPreferencesLastBoardForAccount_ + account.getId());
                editor.remove(sharedPreferencesLastBoardForAccount_ + account.getId());
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    if (entry.getKey().startsWith(sharedPreferencesLastStackForAccountAndBoard_ + account.getId())) {
                        DeckLog.log("--- Remove: " + entry.getKey());
                        editor.remove(entry.getKey());
                    }
                }
                editor.apply();

                context.onAccountDeleted(account.getId());
            });
            m.setActionView(contextMenu);
        }
        menu.add(Menu.NONE, MENU_ID_ADD_ACCOUNT, Menu.NONE, context.getString(R.string.add_account)).setIcon(R.drawable.ic_person_add_black_24dp);
    }

    public static <T extends FragmentActivity & DrawerBoardListener> void inflateBoards(
            @NonNull T context,
            @NonNull Menu menu,
            @NonNull Long currentAccountId,
            long currentBoardId,
            @NonNull List<Board> boards) {
        final String addBoard = context.getString(R.string.add_board);
        final String simpleBoards = context.getString(R.string.simple_boards);
        final String simpleSettings = context.getString(R.string.simple_settings);
        final String about = context.getString(R.string.about);
        final String shareBoard = context.getString(R.string.share_board);
        final String editBoard = context.getString(R.string.edit_board);

        SubMenu boardsMenu = menu.addSubMenu(simpleBoards);
        int index = 0;
        for (Board board : boards) {
            final int currentIndex = index;
            MenuItem m = boardsMenu.add(Menu.NONE, index++, Menu.NONE, board.getTitle()).setIcon(ViewUtil.getTintedImageView(context, R.drawable.circle_grey600_36dp, "#" + board.getColor()));
            if (board.isPermissionManage()) {
                AppCompatImageButton contextMenu = new AppCompatImageButton(context);
                contextMenu.setBackgroundDrawable(null);
                contextMenu.setImageDrawable(ViewUtil.getTintedImageView(context, R.drawable.ic_menu, R.color.grey600));
                contextMenu.setOnClickListener((v) -> {
                    PopupMenu popup = new PopupMenu(context, contextMenu);
                    popup.getMenuInflater().inflate(R.menu.navigation_context_menu, popup.getMenu());
                    final int SHARE_BOARD_ID = -1;
                    if (board.isPermissionShare()) {
                        MenuItem shareItem = popup.getMenu().add(Menu.NONE, SHARE_BOARD_ID, 5, R.string.share_board);
                    }
                    popup.setOnMenuItemClickListener((MenuItem item) -> {
                        switch (item.getItemId()) {
                            case SHARE_BOARD_ID:
                                AccessControlDialogFragment.newInstance(currentAccountId, board.getLocalId()).show(context.getSupportFragmentManager(), shareBoard);
                                break;
                            case R.id.edit_board:
                                EditBoardDialogFragment.newInstance(currentAccountId, board.getLocalId()).show(context.getSupportFragmentManager(), editBoard);
                                break;
                            case R.id.archive_board:
                                // TODO implement
                                Toast.makeText(context, "Archiving boards is not yet supported.", Toast.LENGTH_LONG).show();
                                break;
                            case R.id.delete_board:
                                new DeleteDialogBuilder(context)
                                        .setTitle(context.getString(R.string.delete_something, board.getTitle()))
                                        .setMessage(R.string.delete_board_message)
                                        .setPositiveButton(R.string.simple_delete, (dialog, which) -> {
                                            if (board.getLocalId() == currentBoardId) {
                                                if (currentIndex > 0) { // Select first board after deletion
                                                    context.onBoardChosen(boards.get(0));
                                                } else if (boards.size() > 1) { // Select second board after deletion
                                                    context.onBoardChosen(boards.get(1));
                                                } else { // No other board is available, open create dialog
                                                    context.onLastBoardDeleted();
                                                }
                                            }
                                            context.onBoardDeleted(board);
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show();
                                break;
                        }
                        return true;
                    });
                    popup.show();
                });
                m.setActionView(contextMenu);
            } else if (board.isPermissionShare()) {
                AppCompatImageButton contextMenu = new AppCompatImageButton(context);
                contextMenu.setBackgroundDrawable(null);
                contextMenu.setImageDrawable(ViewUtil.getTintedImageView(context, R.drawable.ic_share_grey600_18dp, R.color.grey600));
                contextMenu.setOnClickListener((v) -> AccessControlDialogFragment.newInstance(currentAccountId, board.getLocalId()).show(context.getSupportFragmentManager(), shareBoard));
                m.setActionView(contextMenu);
            }
        }

        boardsMenu.add(Menu.NONE, MENU_ID_ADD_BOARD, Menu.NONE, addBoard).setIcon(R.drawable.ic_add_grey_24dp);

        menu.add(Menu.NONE, MENU_ID_SETTINGS, Menu.NONE, simpleSettings).setIcon(R.drawable.ic_settings_grey600_24dp);
        menu.add(Menu.NONE, MENU_ID_ABOUT, Menu.NONE, about).setIcon(R.drawable.ic_info_outline_grey_24dp);
    }

    public interface DrawerAccountListener {
        void onAccountChosen(@NonNull Account account);

        void onAccountDeleted(@NonNull Long accountId);
    }

    public interface DrawerBoardListener {
        void onBoardChosen(@NonNull Board board);

        void onBoardDeleted(@NonNull Board board);

        default void onLastBoardDeleted() {
        }
    }
}
