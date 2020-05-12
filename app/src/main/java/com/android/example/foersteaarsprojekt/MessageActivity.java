package com.android.example.foersteaarsprojekt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
    private TextView textView;
    private RecyclerView recyclerView;
    private EditText editText;
    private MessageAdapter messageAdapter;
    private Query query;
    private String practitionerOrClient;

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
        }
        else {
            receiverType = "behandlere";
            textView.setText("Klient " + mAuth.getCurrentUser().getEmail());
            query = db.collection("chat").whereEqualTo("klient", mAuth.getCurrentUser().getEmail());
            practitionerOrClient = "klient";
        }

        query = db.collection("chat").whereEqualTo(practitionerOrClient, mAuth.getCurrentUser().getEmail());

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                final ArrayList<Message> forAdapter = new ArrayList<>();
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    documentSnapshot.getReference().collection("messages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> messages = task.getResult().getDocuments();
                                for (int i = 0; i < messages.size(); i++) {
                                    String message = (String) messages.get(i).get("message");
                                    String author = (String) messages.get(i).get("author");
                                    Message messageObject = new Message(author, message);
                                    forAdapter.add(messageObject);
                                }
                            }
                        }
                    });
                }
                messageAdapter.setMessages(forAdapter);
            }
        });

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


        // This works
//        db.collection("chat").whereEqualTo("recipient", mAuth.getCurrentUser().getEmail()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if (task.isSuccessful()) {
//                    final ArrayList<Message> recycle = new ArrayList<>();
//                    List<DocumentSnapshot> documents = task.getResult().getDocuments();
//                    for (int i = 0; i < documents.size(); i++) {
//                        documents.get(i).getReference().collection("messages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                List<DocumentSnapshot> messages = task.getResult().getDocuments();
//                                for (int j = 0; j < messages.size(); j++) {
//                                    String message = (String) messages.get(j).get("message");
//                                    String author = (String) messages.get(j).get("author");
//                                    Message object = new Message();
//                                    object.setSender(author);
//                                    object.setMessage(message);
//                                    recycle.add(object);
//                                }
//                            }
//                        });
//                    }
//                    messageAdapter.setMessages(recycle);
//                }
//            }
//        });
//
       recyclerView.setAdapter(messageAdapter);
    }

    public void send(View view) {
        String where = "";
        if (practitionerOrClient.equals("behandler")) {
            where = "klient";
        }
        else {
            where = "behandler";
        }
        Query query = db.collection("chat").whereEqualTo(where, "test@hotmail.com").whereEqualTo(practitionerOrClient, mAuth.getCurrentUser().getEmail());
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("message", editText.getText().toString());
                    message.put("author", mAuth.getCurrentUser().getEmail());
                    message.put("recipient", "test@hotmail.com");
                    task.getResult().getDocuments().get(0).getReference().collection("messages").add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("test4", "Success");
                        }
                    });
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
