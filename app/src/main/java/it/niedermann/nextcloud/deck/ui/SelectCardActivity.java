package it.niedermann.nextcloud.deck.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import it.niedermann.nextcloud.deck.DeckLog;
import it.niedermann.nextcloud.deck.R;
import it.niedermann.nextcloud.deck.exceptions.UploadAttachmentFailedException;
import it.niedermann.nextcloud.deck.model.Account;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.full.FullCard;
import it.niedermann.nextcloud.deck.model.ocs.comment.DeckComment;
import it.niedermann.nextcloud.deck.ui.branding.BrandedAlertDialogBuilder;
import it.niedermann.nextcloud.deck.ui.card.SelectCardListener;
import it.niedermann.nextcloud.deck.util.ExceptionUtil;
import it.niedermann.nextcloud.deck.util.MimeTypeUtil;

import static it.niedermann.nextcloud.deck.util.AttachmentUtil.copyContentUriToTempFile;
import static it.niedermann.nextcloud.deck.util.ClipboardUtil.copyToClipboard;
import static it.niedermann.nextcloud.deck.util.MimeTypeUtil.APPLICATION_OCTET_STREAM;

public class SelectCardActivity extends MainActivity implements SelectCardListener {

    private boolean isFile;

    private String receivedText;
    @NonNull
    List<Parcelable> mStreamsToUpload = new ArrayList<>(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            final Intent receivedIntent = getIntent();
            final String receivedAction = receivedIntent.getAction();
            final String receivedType = receivedIntent.getType();
            DeckLog.info(receivedAction);
            DeckLog.info(receivedType);
            isFile = !MimeTypeUtil.isText(receivedType);
            if (isFile) {
                if (Intent.ACTION_SEND.equals(receivedIntent.getAction())) {
                    mStreamsToUpload = Collections.singletonList(receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM));
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(receivedIntent.getAction())) {
                    @Nullable List<Parcelable> listOfParcelables = receivedIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    if (listOfParcelables != null) {
                        mStreamsToUpload.addAll(listOfParcelables);
                    }
                } else {
                    new BrandedAlertDialogBuilder(this)
                            .setTitle(R.string.error)
                            .setMessage(R.string.operation_not_yet_supported)
                            .setPositiveButton(R.string.simple_close, (a, b) -> finish())
                            .create().show();
                }
            } else {
                receivedText = receivedIntent.getStringExtra(Intent.EXTRA_TEXT);
                binding.toolbar.setSubtitle(receivedText);
            }
        } catch (Throwable throwable) {
            handleException(throwable);
        }
    }

    @Override
    public void onCardSelected(FullCard fullCard) {
        try {
            if (isFile) {
                for (Parcelable sourceStream : mStreamsToUpload) {
                    // TODO How to listen to all those threads finished?
                    new Thread(() -> {
                        if (!(sourceStream instanceof Uri)) {
                            handleExceptionFromBackgroundThread(new UploadAttachmentFailedException("stream is not of type " + Uri.class.getSimpleName()));
                            return;
                        }
                        final Uri uri = (Uri) sourceStream;
                        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                            Toast.makeText(this, "", Toast.LENGTH_LONG).show();
                            handleExceptionFromBackgroundThread(new UploadAttachmentFailedException("Unhandled URI scheme: " + uri.getScheme()));
                            return;
                        }

                        try {
                            File copiedFile = copyContentUriToTempFile(this, uri, fullCard.getAccountId(), fullCard.getCard().getLocalId());
                            String mimeType = getContentResolver().getType(uri);
                            if (mimeType == null) {
                                mimeType = APPLICATION_OCTET_STREAM;
                            }
                            syncManager.addAttachmentToCard(fullCard.getAccountId(), fullCard.getCard().getLocalId(), mimeType, copiedFile);
                        } catch (IOException | IllegalArgumentException e) {
                            handleExceptionFromBackgroundThread(new UploadAttachmentFailedException("Error while uploading attachment", e));
                        }
                    }).start();
                }
                finish();
            } else {
                appendText(fullCard, receivedText);
            }
        } catch (Throwable throwable) {
            handleException(throwable);
        }
    }

    private void handleExceptionFromBackgroundThread(@NonNull Throwable throwable) {
        DeckLog.logError(throwable);
        Toast.makeText(this, getString(R.string.error_while_uploading_attachment, throwable.getLocalizedMessage()), Toast.LENGTH_LONG).show();
    }

    private void handleException(@NonNull Throwable throwable) {
        DeckLog.logError(throwable);
        String debugInfos = ExceptionUtil.getDebugInfos(this, throwable, mainViewModel.getCurrentAccount());
        final AlertDialog dialog = new BrandedAlertDialogBuilder(this)
                .setTitle(R.string.error)
                .setMessage(debugInfos)
                .setPositiveButton(android.R.string.copy, (a, b) -> {
                    copyToClipboard(this, throwable.getMessage(), "```\n" + debugInfos + "\n```");
                    finish();
                })
                .setNeutralButton(R.string.simple_close, null)
                .create();
        dialog.show();
        ((TextView) Objects.requireNonNull(dialog.findViewById(android.R.id.message))).setTypeface(Typeface.MONOSPACE);
    }

    private void appendText(@NonNull FullCard fullCard, @NonNull String receivedText) {
        final String[] animals = {getString(R.string.append_text_to_description), getString(R.string.add_text_as_comment)};
        new BrandedAlertDialogBuilder(this)
                .setItems(animals, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            final String oldDescription = fullCard.getCard().getDescription();
                            DeckLog.info("Adding to card #" + fullCard.getCard().getId() + " (" + fullCard.getCard().getTitle() + "): Text \"" + receivedText + "\"");
                            fullCard.getCard().setDescription(
                                    (oldDescription == null || oldDescription.length() == 0)
                                            ? receivedText
                                            : oldDescription + "\n\n" + receivedText
                            );
                            // TODO check return value
                            syncManager.updateCard(fullCard);
                            Toast.makeText(getApplicationContext(), getString(R.string.share_success, "\"" + receivedText + "\"", "\"" + fullCard.getCard().getTitle() + "\""), Toast.LENGTH_LONG).show();
                            finish();
                            break;
                        case 1:
                            final Account currentAccount = mainViewModel.getCurrentAccount();
                            final DeckComment comment = new DeckComment(receivedText.trim(), currentAccount.getUserName(), new Date());
                            syncManager.addCommentToCard(currentAccount.getId(), fullCard.getLocalId(), comment);
                            Toast.makeText(getApplicationContext(), getString(R.string.share_success, "\"" + receivedText + "\"", "\"" + fullCard.getCard().getTitle() + "\""), Toast.LENGTH_LONG).show();
                            finish();
                            break;
                    }
                }).create().show();
    }

    @Override
    protected void setCurrentBoard(@NonNull Board board) {
        super.setCurrentBoard(board);
        binding.listMenuButton.setVisibility(View.GONE);
        binding.fab.setVisibility(View.GONE);
        binding.toolbar.setTitle(R.string.simple_select);
    }

    @Override
    protected void showFabIfEditPermissionGranted() { /* Silence is gold */ }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}