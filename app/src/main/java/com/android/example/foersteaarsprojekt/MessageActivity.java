package com.android.example.foersteaarsprojekt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageActivity extends Activity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Spinner spinner;
    private ArrayList<String> list;
    private ArrayList<Message> currentMessages;
    private TextView textView;
    private RecyclerView recyclerView;
    private EditText editText;
    private MessageAdapter messageAdapter;
    private Query query;
    private String practitionerOrClient;
    private String opposite;

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
        messageAdapter = new MessageAdapter();


        Intent intent = getIntent();
        String type = intent.getStringExtra("type");

        String receiverType;
        if (type.equals("behandlere")) {
            receiverType = "klienter";
            textView.setText("Behandler " + mAuth.getCurrentUser().getEmail());
            practitionerOrClient = "behandler";
            opposite = "klient";
        }
        else {
            receiverType = "behandlere";
            textView.setText("Klient " + mAuth.getCurrentUser().getEmail());
            query = db.collection("chat").whereEqualTo("klient", mAuth.getCurrentUser().getEmail());
            practitionerOrClient = "klient";
            opposite = "behandler";
        }

        currentMessages = new ArrayList<>();
//        query = db.collection("chat").whereEqualTo(practitionerOrClient, mAuth.getCurrentUser().getEmail()).whereEqualTo(opposite, spinner.getSelectedItem());
//        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
//            @Override
//            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
//                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                    documentSnapshot.getReference().collection("messages").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
//                        @Override
//                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
//                            List<DocumentSnapshot> messages = queryDocumentSnapshots.getDocuments();
//                            currentMessages.clear();
//                            for (DocumentSnapshot snapshot : messages) {
//                                String message = (String) snapshot.get("message");
//                                String author = (String) snapshot.get("author");
//                                Message messageObject = new Message(author, message);
//                                currentMessages.add(messageObject);
//                            }
//                            messageAdapter.setMessages(currentMessages);
//                        }
//                    });
//                }
//
//            }
//        });

        list = new ArrayList<>();
        db.collection(receiverType).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot> receivers = task.getResult().getDocuments();
                for (int i = 0; i < receivers.size(); i++) {
                    String string = receivers.get(i).getId();
                    list.add(string);
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, list);
                spinner.setAdapter(arrayAdapter);
            }
        });

        recyclerView.setAdapter(messageAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newRecipient = parent.getItemAtPosition(position).toString();
                updateRecipient(newRecipient);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void updateRecipient(String newRecipient) {
        Query query = db.collection("chat").whereEqualTo(practitionerOrClient, mAuth.getCurrentUser().getEmail()).whereEqualTo(opposite, newRecipient);
        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (!queryDocumentSnapshots.isEmpty()) {
                    queryDocumentSnapshots.getDocuments().get(0).getReference().collection("messages").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentSnapshot> messages = queryDocumentSnapshots.getDocuments();
                            currentMessages.clear();
                            for (DocumentSnapshot snapshot : messages) {
                                String message = (String) snapshot.get("message");
                                String author = (String) snapshot.get("author");
                                Message messageObject = new Message(author, message);
                                currentMessages.add(messageObject);
                            }
                            messageAdapter.setMessages(currentMessages);
                        }
                    });
                }
            }
        });
    }

    public void send(View view) {
        String where = "";
        if (practitionerOrClient.equals("behandler")) {
            where = "klient";
        }
        else {
            where = "behandler";
        }
        Query query = db.collection("chat").whereEqualTo(where, spinner.getSelectedItem().toString()).whereEqualTo(practitionerOrClient, mAuth.getCurrentUser().getEmail());
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        Map<String, Object> newChat = new HashMap<>();
                        if (practitionerOrClient.equals("behandler")) {
                            newChat.put("behandler", mAuth.getCurrentUser().getEmail());
                            newChat.put("klient", spinner.getSelectedItem().toString());
                        }
                        else {
                            newChat.put("behandler", spinner.getSelectedItem().toString());
                            newChat.put("klient", mAuth.getCurrentUser().getEmail());
                        }
                        DocumentReference newDoc = db.collection("chat").document();
                        Map<String, Object> newMessage = new HashMap<>();
                        newMessage.put("message", editText.getText().toString());
                        newMessage.put("author", mAuth.getCurrentUser().getEmail());
                        newMessage.put("recipient", spinner.getSelectedItem().toString());
                        newMessage.put("timestamp", Timestamp.now());
                        newDoc.collection("messages").add(newMessage);
                        newDoc.set(newChat);

                    }
                    else {
                        Map<String, Object> message = new HashMap<>();
                        message.put("message", editText.getText().toString());
                        message.put("author", mAuth.getCurrentUser().getEmail());
                        message.put("recipient", spinner.getSelectedItem().toString());
                        message.put("timestamp", Timestamp.now());
                        task.getResult().getDocuments().get(0).getReference().collection("messages").add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("test4", "Success");
                            }
                        });
                    }
                }
            }
        });
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
