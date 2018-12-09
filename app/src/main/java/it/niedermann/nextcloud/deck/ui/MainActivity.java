package it.niedermann.nextcloud.deck.ui;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.api.IResponseCallback;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.persistence.sync.SyncManager;
import it.niedermann.nextcloud.deck.ui.card.CardAdapter;
import it.niedermann.nextcloud.deck.ui.login.LoginDialogFragment;
import it.niedermann.nextcloud.deck.ui.stack.StackAdapter;
import it.niedermann.nextcloud.deck.ui.stack.StackFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int MENU_ID_ABOUT = -1;
    private static final int ACTIVITY_ABOUT = 1;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.navigationView) NavigationView navigationView;
    @BindView(R.id.stackLayout) TabLayout stackLayout;
    @BindView(R.id.viewPager) ViewPager viewPager;

    private StackAdapter stackAdapter;
    private LoginDialogFragment loginDialogFragment;
    private SyncManager syncManager;
    private List<Board> boardsList;
    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        fab.setOnClickListener((View view) -> {
            Snackbar.make(view, "Creating new Cards is not yet supported", Snackbar.LENGTH_LONG).show();
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        syncManager = new SyncManager(getApplicationContext(), this);
        stackAdapter = new StackAdapter(getSupportFragmentManager());


        viewPager.setOnDragListener((View v, DragEvent dragEvent) -> {
            Log.d("Deck", "Drag: "+ dragEvent.getAction());
            if(dragEvent.getAction() == 4)
                Log.d("Deck", dragEvent.getAction() + "");

            View view = (View) dragEvent.getLocalState();
            RecyclerView owner = (RecyclerView) view.getParent();
            CardAdapter cardAdapter = (CardAdapter) owner.getAdapter();

            switch(dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_LOCATION:
                    Point size = new Point();
                    getWindowManager().getDefaultDisplay().getSize(size);
                    if(dragEvent.getX() <= 20) {
                        Log.d("Deck", dragEvent.getAction() + " moved left");
                        viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                    } else if(dragEvent.getX() >= size.x - 20) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                        Log.d("Deck", dragEvent.getAction() + " moved right");
                    }
                    int viewUnderPosition = owner.getChildAdapterPosition(owner.findChildViewUnder(dragEvent.getX(), dragEvent.getY()));
                    if(viewUnderPosition != -1) {
                        Log.d("Deck", dragEvent.getAction() + " moved something...");
                        cardAdapter.moveItem(owner.getChildLayoutPosition(view), viewUnderPosition);
                    }
                    break;
                case DragEvent.ACTION_DROP:
                    view.setVisibility(View.VISIBLE);
                    break;
            }
            return true;
        });

        if(this.syncManager.hasAccounts()) {
            account = syncManager.readAccounts().get(0);
            String accountName = account.getName();
            SingleAccountHelper.setCurrentAccount(getApplicationContext(), accountName);

            syncManager.getBoards(account.getId(), new IResponseCallback<List<Board>>(account) {
                @Override
                public void onResponse(List<Board> boards) {
                    Menu menu = navigationView.getMenu();
                    SubMenu boardsMenu = menu.addSubMenu(getString(R.string.simple_boards));
                    boardsList = boards;
                    int index = 0;
                    for(Board board: boardsList) {
                        boardsMenu.add(Menu.NONE, index++, Menu.NONE, board.getTitle()).setIcon(R.drawable.ic_view_column_black_24dp);
                    }
                    menu.add(Menu.NONE, MENU_ID_ABOUT, Menu.NONE, getString(R.string.about)).setIcon(R.drawable.ic_info_outline_black_24dp);
                    if (boardsList.size()>0){
                        displayStacksForIndex(0, account);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        } else {
            loginDialogFragment = new LoginDialogFragment();
            loginDialogFragment.show(this.getSupportFragmentManager(), "NoticeDialogFragment");
        }
    }

    public void onAccountChoose(SingleSignOnAccount account) {
        getSupportFragmentManager().beginTransaction().remove(loginDialogFragment).commit();
        this.syncManager.createAccount(account.name);
    }

    /**
     * Displays the Stacks for the boardsList by index
     * @param index of boardsList
     */
    private void displayStacksForIndex(int index, Account account) {
        Board selectedBoard = boardsList.get(index);
        if(toolbar != null) {
            toolbar.setTitle(selectedBoard.getTitle());
        }
        syncManager.getStacks(account.getId(), selectedBoard.getLocalId(), new IResponseCallback<List<Stack>>(account) {
            @Override
            public void onError(Throwable throwable) {
                Log.e("Deck", throwable.getMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onResponse(List<Stack> response) {
                stackAdapter.clear();
                for(Stack stack: response) {
                    stackAdapter.addFragment(StackFragment.newInstance(selectedBoard.getLocalId(), stack.getLocalId()), stack.getTitle());
                }
                runOnUiThread(() -> {
                    viewPager.setAdapter(stackAdapter);
                    stackLayout.setupWithViewPager(viewPager);
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case MENU_ID_ABOUT:
                Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivityForResult(aboutIntent, ACTIVITY_ABOUT);
                break;
            default:
                displayStacksForIndex(item.getItemId(), account);
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
