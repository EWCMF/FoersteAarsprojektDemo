package com.android.example.foersteaarsprojekt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MessageActivity extends Activity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Spinner spinner;
    private ArrayList<String> list;
    private TextView textView;
    private RecyclerView recyclerView;
    private EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        spinner = findViewById(R.id.spinner3);
        textView = findViewById(R.id.textView3);
        recyclerView = findViewById(R.id.messageRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        editText = findViewById(R.id.messageEditText);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");

        String receiverType;
        if (type.equals("behandlere")) {
            receiverType = "klienter";
            textView.setText("Behandler " + mAuth.getCurrentUser().getEmail());
        }
        else {
            receiverType = "behandlere";
            textView.setText("Klient " + mAuth.getCurrentUser().getEmail());
        }

        list = new ArrayList<>();
        db.collection(receiverType).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot> receivers = task.getResult().getDocuments();
                for (int i = 0; i < receivers.size(); i++) {
                    String add = receivers.get(i).getId();
                    list.add(add);
                }
            }
        });

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, list);
        spinner.setAdapter(arrayAdapter);

        final MessageAdapter messageAdapter = new MessageAdapter();

        Query query = db.collectionGroup("messages").whereEqualTo("receiver", mAuth.getCurrentUser().getEmail());
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                List<DocumentSnapshot> snapshotsDocuments = queryDocumentSnapshots.getDocuments();
                ArrayList<Message> messages = new ArrayList<>();
                for (DocumentSnapshot snapshot : snapshotsDocuments) {
                    if (snapshot.exists()) {
                        Message message = snapshot.toObject(Message.class);
                        message.setMessage((String) snapshot.get("message"));
                        message.setSender((String) snapshot.get("author"));
                        messages.add(message);
                    }
                }
                messageAdapter.setMessages(messages);
            }
        });

        recyclerView.setAdapter(messageAdapter);

    }

    public void send(View view) {

    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private List<Message> messages = new ArrayList<>();


        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message currentMessage = messages.get(position);
            holder.author.setText(currentMessage.getSender());
            holder.message.setText(currentMessage.getMessage());
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public void setMessages(List<Message> messages) {
            this.messages = messages;
            notifyDataSetChanged();
        }
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView author;
        private TextView message;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            author = itemView.findViewById(R.id.messageAuthor);
            message = itemView.findViewById(R.id.messageMessage);
        }
    }
}
