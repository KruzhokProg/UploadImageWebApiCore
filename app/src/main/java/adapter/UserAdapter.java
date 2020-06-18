package adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.uploadimagewebapicore.R;

import java.util.List;
import Model.User;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import helpers.DbBitmapUtility;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder>{

    Context context;
    List<User> data;

    public UserAdapter(Context context, List<User> data) {
        this.context = context;
        this.data = data;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_row, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.tvName.setText(data.get(position).getName());
//        Bitmap image = DbBitmapUtility.getBytes()
        String imagePath = "http://vongucore-001-site1.dtempurl.com/api/image/" + data.get(position).getName();
//        String imagePath = "https://image.tmdb.org/t/p/w500/wwemzKWzjKYJFfCeiB57q3r4Bcm.png";
        Uri myUri = Uri.parse(imagePath);
        Glide.with(context).load(imagePath).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder{

        TextView tvName;
        ImageView image;

        public UserViewHolder(@NonNull View view) {
            super(view);

            tvName = view.findViewById(R.id.tvName);
            image = view.findViewById(R.id.imgv);
        }
    }
}
