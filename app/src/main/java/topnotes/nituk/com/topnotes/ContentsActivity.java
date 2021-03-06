package topnotes.nituk.com.topnotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ContentsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ContentAdapter mContentAdapter;
    private List<Content> fetchedContentList;
    private Content mContent;
    private FirebaseStorage firebaseStorage;
    private int choosenSubject;
    private int choosenType;
    private String subjectTokenArray[];
    private String typeTokenArray[];
    private DbHelper dbHelper;
    private boolean isInDB;
    private List<Content> localContentList;
    private FrameLayout fragmeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getIntent().getIntExtra("theme",0));//it will change the theme of activity
        setContentView(R.layout.activity_contents);


        fragmeLayout = findViewById(R.id.contentsFrameLayout);

        choosenSubject= getIntent().getIntExtra("subject",0);
        choosenType=getIntent().getIntExtra("type",0);

        //Toast.makeText(this,"Subject:"+choosenSubject+"Type:"+choosenType,Toast.LENGTH_SHORT).show();

        // initialise


        setTitle(getResources().getStringArray(R.array.categoryList)[choosenType]);

        // initialise
        dbHelper = new DbHelper(getApplicationContext());

        fetchedContentList= new ArrayList<>();
        localContentList= dbHelper.readContentList(MyApplication.getApp().subjectNamesToken.get(choosenSubject),getResources().getStringArray(R.array.typeToken)[choosenType]);

        // clear this old list from database , and later will put the fresh fetched list into it.
        dbHelper.deleteContentList(MyApplication.getApp().subjectNamesToken.get(choosenSubject),getResources().getStringArray(R.array.typeToken)[choosenType]);

        Log.i("fromdb:",""+localContentList.size());

        //initialising array from string .xml res file
        //subjectTokenArray=getResources().getStringArray(R.array.subjectToken);
        subjectTokenArray = new String[MyApplication.getApp().subjectNamesToken.size()];
        subjectTokenArray=MyApplication.getApp().subjectNamesToken.toArray(subjectTokenArray);
        typeTokenArray=getResources().getStringArray(R.array.typeToken);

        mRecyclerView= findViewById(R.id.contentsRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // get the firebase storage
        firebaseStorage = FirebaseStorage.getInstance();

        // retrieve the choosen subject and choosen type from the intent

        loadContent();

        updateUI();

        Log.i("onCreate::","withing contentActivity");

    }
    

    // ViewHolder for the recycler view which inflates our own view
    private class ContentHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView,mAuthorTextView,mDateTextView;

        ContentHolder(LayoutInflater inflater, ViewGroup container)
       {
           super(inflater.inflate(R.layout.recyclerview_content_raw_layout,container,false));

           itemView.setOnClickListener(this);
           // get reference to the views using the viewholder when the viewholders are created here

           mTitleTextView=itemView.findViewById(R.id.recyclerNotesNameTextView);
           mAuthorTextView=itemView.findViewById(R.id.recyclerAuthorNameTextView);
           mDateTextView=itemView.findViewById(R.id.uploadDateTextView);

       }

        // The method binds the data to the viewholder
        public void bind(Content content)
        {
            mContent = content;
            // bind your data to the views here
            mTitleTextView.setText(content.getTitle());
            mAuthorTextView.setText("Author : "+content.getAuthor());
            mDateTextView.setText("Upload Date : "+content.getDate());
        }

        // implement the recycler view list item click action here
        @Override
        public void onClick(View view) {

            //getAdapterPosition will returns the position of the item clicked on the recycler view;
            DownloadDialogFragment.getInstance(localContentList
                    .get(getAdapterPosition()),choosenSubject,choosenType)
                    .show(getSupportFragmentManager(),"Download");

            }

    }
    // Adapter for recycler view

    private class ContentAdapter extends RecyclerView.Adapter<ContentHolder>  {

        private List mContents;
        public ContentAdapter(List<Content> contents)
        {
           mContents =  contents;
        }

        // second method that will be called
        @NonNull
        @Override
        public ContentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(ContentsActivity.this);
            return new ContentHolder(layoutInflater,parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ContentHolder holder, int position) {
          Content content = (Content)mContents.get(position);
          holder.bind(content);

        }

        // first this method will  call
        @Override
        public int getItemCount() {
            return mContents.size();
        }


    }
    // The method gets the list of all Content objects
    public void updateUI()
    {
//       ContentLab contentLab = ContentLab.getInstance(this);
//       List<Content> contents = contentLab.getContents();
//       Log.i("updating ui",contents.toString());
       mContentAdapter = new ContentAdapter(localContentList);
       mRecyclerView.setAdapter(mContentAdapter);
    }

    // The method targets a metadata request from FirebaseDB or Sqlite for a content
    public void loadContent()
    {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("courses");
        DatabaseReference ref=databaseReference.child(subjectTokenArray[choosenSubject])
                .child(typeTokenArray[choosenType]);

                ref.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Content content=dataSnapshot.getValue(Content.class);//it will return the
                        // Content object since we have set
                        // the values as a content and we have passed the argument as a Content class
                        if(content!=null)
                        {   Log.i("note id:",dataSnapshot.getKey());
                            Log.i("fetched:",content.getTitle()+" "+content.getAuthor()+" "+content.getDate());
                          fetchedContentList.add(content);
                          if(!localContentList.contains(content))
                          {
                              localContentList.add(content);
                              mContentAdapter.notifyDataSetChanged();
                          }
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //this will be called when all the child is added in array list
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Toast.makeText(ContentsActivity.this,"Fetching done!",Toast.LENGTH_SHORT).show();
                        Log.i("localsize:",""+localContentList.size());
                        refreshDownloadsCount();
                        addToDB();
                        showNOItemPage();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



    }

    private void showNOItemPage() {
        if(localContentList.size()==0)
        {
            mRecyclerView.setVisibility(View.INVISIBLE);
            fragmeLayout.setVisibility(View.VISIBLE);
            try {
                getSupportFragmentManager().beginTransaction().replace(R.id.contentsFrameLayout,
                        new NothingToDisplayDialogFragment("download", "No Items till now in this section")).commit();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    public void addToDB()
    {
        dbHelper.saveContentList(fetchedContentList,MyApplication.getApp().subjectNamesToken.get(choosenSubject),getResources().getStringArray(R.array.typeToken)[choosenType]);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void refreshDownloadsCount()
    {
        for(int i=0;i<fetchedContentList.size();i++)
        {
            Content fetchedContent = fetchedContentList.get(i);
            Content localContent = localContentList.get(i);

            if(localContent.getDownloads()!=fetchedContent.getDownloads())
            {
                localContent.setDownloads(fetchedContent.getDownloads());
            }
        }

        mContentAdapter.notifyDataSetChanged();
    }

}
