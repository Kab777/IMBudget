package com.junyu.IMBudget.fragment;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.DashPathEffect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.junyu.IMBudget.MMConstant;
import com.junyu.IMBudget.MMUserPreference;
import com.junyu.IMBudget.R;
import com.junyu.IMBudget.activity.ActivityFriendChat;
import com.junyu.IMBudget.dialog.SendFriendRequestDialog;
import com.junyu.IMBudget.model.Friend;
import com.junyu.IMBudget.model.MessageChatModel;


import com.pnikosis.materialishprogress.ProgressWheel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import timber.log.Timber;

import static android.R.attr.id;
import static android.R.attr.x;
import static android.media.CamcorderProfile.get;
import static com.junyu.IMBudget.MMConstant.CHAT_ID;
import static com.junyu.IMBudget.MMConstant.FRIENDS;
import static com.junyu.IMBudget.MMConstant.MESSAGES;
import static com.junyu.IMBudget.MMConstant.NAME;
import static com.junyu.IMBudget.MMConstant.USER_ID;
import static com.junyu.IMBudget.R.color.onlineStatus;
import static com.junyu.IMBudget.utils.Time.formateDateFromFriendScreen;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentChat extends Fragment {
    @BindView(R.id.rvFriends) RecyclerView rvFriends;
    @BindView(R.id.addFriend) FloatingActionButton addFriend;
    @BindView(R.id.progress) ProgressWheel progressWheel;


    @OnClick(R.id.addFriend)
    public void addFriend() {
        //A dialog that sends friend adding request
        SendFriendRequestDialog dialog = new SendFriendRequestDialog(getContext());
        dialog.show();
    }


    private ArrayList<Friend> friends = new ArrayList<>();
    private HashMap<String, Integer> friendsIdPMap = new HashMap<>();
    private DatabaseReference fireDb;
    private FriendAdapter friendAdapter;
    private String userId;
    private Integer userCounter = 0;

    @Override
    public String toString() {
        return super.toString();
    }

    public FragmentChat() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, view);
        friendAdapter = new FriendAdapter(getContext(), friends);
        rvFriends.setAdapter(friendAdapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        progressWheel.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fireDb = FirebaseDatabase.getInstance().getReference();
        userId = MMUserPreference.getUserId(getContext());
        DatabaseReference friendDb = fireDb.child(MMConstant.USERS).child(userId).child(FRIENDS);

        friendDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Timber.v(dataSnapshot.toString());
                progressWheel.setVisibility(View.GONE);

                final Friend newFriend = dataSnapshot.getValue(Friend.class);
                friendsIdPMap.put(newFriend.getChatId(), userCounter);
                friends.add(newFriend);
                friendAdapter.notifyItemInserted(userCounter);
                progressWheel.setVisibility(View.GONE);
                userCounter++;

                Query lastMsg = fireDb.child(MESSAGES).child(newFriend.getChatId()).limitToLast(1);
                lastMsg.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // received new msg
                        if (dataSnapshot == null) {
                            newFriend.setLastMsg("");
                        } else {
                            // there is only one child
                            Iterator<DataSnapshot> dataSnap = dataSnapshot.getChildren().iterator();
                            if (dataSnap.hasNext()) {
                                MessageChatModel newMsg = dataSnap.next().getValue(MessageChatModel.class);
                                //find position by chat id
                                int friendPosition = friendsIdPMap.get(dataSnapshot.getKey().toString());
                                friends.get(friendPosition).setLastMsg(newMsg.getContent());
                                friends.get(friendPosition).setLastMsgTime(formateDateFromFriendScreen(newMsg.getTimestamp()));
                                friendAdapter.notifyItemChanged(friendPosition);
                            }


                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //update online status
                Friend changedFriend = dataSnapshot.getValue(Friend.class);
                int friendPosition = friendsIdPMap.get(changedFriend.getChatId());
                Boolean onlineStatus = friends.get(friendPosition).getOnline();
                friends.get(friendPosition).setOnline(!onlineStatus);
                friendAdapter.notifyItemChanged(friendPosition);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
        private List<Friend> friends;
        private Context context;

        public FriendAdapter(Context context, List<Friend> friends) {
            this.friends = friends;
            this.context = context;
        }

        private Context getContext() {
            return context;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.userImage) ImageView userImage;
            @BindView(R.id.userName) TextView userName;
            @BindView(R.id.lastChat) TextView lastChat;
            @BindView(R.id.online) TextView online;
            @BindView(R.id.offline) TextView offline;
            @BindView(R.id.lastMsgTime) TextView lastMsgTime;


            public ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }

        @Override
        public FriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View friendView = inflater.inflate(R.layout.item_friend_row, parent, false);

            // Return a new holder instance
            ViewHolder viewHolder = new ViewHolder(friendView);
            return viewHolder;
        }


        @Override
        public void onBindViewHolder(final FriendAdapter.ViewHolder holder, int position) {
            final Friend friend = friends.get(position);
            Timber.v(friend.toString());
            String userName = friend.getName();
            String userImg = friend.getImgUrl();
            Boolean onlineStatus = friend.getOnline();
            holder.userName.setText(userName);
            if (onlineStatus) {
                holder.online.setVisibility(View.VISIBLE);
                holder.offline.setVisibility(View.GONE);
            } else {
                holder.online.setVisibility(View.GONE);
                holder.offline.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // start chat activity
                    Intent intent = new Intent(getContext(), ActivityFriendChat.class);
                    intent.putExtra(CHAT_ID, friend.getChatId());
                    intent.putExtra(NAME, friend.getName());
                    Bitmap image = ((BitmapDrawable)holder.userImage.getDrawable()).getBitmap();
                    intent.putExtra("image", image);
                    intent.putExtra(USER_ID, friend.getUserId());
                    startActivity(intent);
                }
            });
            if (userImg != null && !userImg.equals("")) {
                Picasso.with(context).load(userImg)
                        .placeholder(R.drawable.ic_face_profile)
                        .into(holder.userImage);
            } else {
                Picasso.with(context)
                        .load(R.drawable.ic_face_profile)
                        .into(holder.userImage);
            }

            holder.lastChat.setText(friend.getLastMsg());
            holder.lastMsgTime.setText(friend.getLastMsgTime());
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }
    }
}
