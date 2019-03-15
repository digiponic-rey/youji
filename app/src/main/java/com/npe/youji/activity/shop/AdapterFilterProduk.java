package com.npe.youji.activity.shop;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.npe.youji.R;
import com.npe.youji.model.dbsqlite.CartOperations;
import com.npe.youji.model.dbsqlite.ShopOperations;
import com.npe.youji.model.shop.CartModel;
import com.npe.youji.model.shop.JoinModel;

import java.util.List;

public class AdapterFilterProduk extends RecyclerView.Adapter<AdapterFilterProduk.ViewHolder> {
    private Context context;
    private List<JoinModel> items;
    CartOperations cartOperations;
    ShopOperations shopOperations;
    private Gson gson;
    private OnItemClickListener mListener;

    public AdapterFilterProduk(Context context, List<JoinModel> items) {
        this.context = context;
        this.items = items;
        cartOperations = new CartOperations(context);
        shopOperations = new ShopOperations(context);
    }

    public interface OnItemClickListener {
        void onItemCickFilter(int position, JoinModel data);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_shop_item_list_detail, viewGroup, false);
        return new ViewHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {
        RequestOptions myOptions = new RequestOptions()
                .fitCenter() // or centerCrop
                .override(100, 100);

        final JoinModel data = items.get(i);
        Glide.with(context)
                .load(data.getGambar())
                .apply(myOptions)
                .into(viewHolder.imageView);
        viewHolder.nama.setText(data.getKeterangan());
        viewHolder.harga.setText(String.valueOf(data.getHarga()));
        viewHolder.label.setText(String.valueOf(data.getKategori()));
        //check quantity
        if (checkQuantity(i) > 0) {
            Log.i("QuantityBarang", "LebihDari0");
            displayExist(viewHolder, i);
            showLayoutCart(viewHolder, data, i);
        }
        if (data.getStok() == 0) {
            viewHolder.beli.setVisibility(View.GONE);
            viewHolder.textStokNull.setVisibility(View.VISIBLE);
        } else {
            viewHolder.beli.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLayoutCart(viewHolder, data, i);
                }
            });
        }
    }

    private void displayExist(ViewHolder viewHolder, int i) {
        viewHolder.textQuantity.setText(String.valueOf(checkQuantity(i)));
    }

    private void showLayoutCart(final ViewHolder viewHolder, final JoinModel data, final int position) {
        viewHolder.layoutCart.setVisibility(View.VISIBLE);
        viewHolder.beli.setVisibility(View.GONE);
        //insert data to cart
        if (checkQuantity(position) == 0) {
            //Toast.makeText(context, "Check", Toast.LENGTH_SHORT).show();
            insertFirst(position, 1);
        }
        //add
        viewHolder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCart(viewHolder, data, position);
            }
        });
        //minus
        viewHolder.btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minusCart(viewHolder, data, position);
            }
        });
    }

    private void addCart(ViewHolder viewHolder, JoinModel data, int position) {
        //insert table cart and update table shop
        String strQuantity = String.valueOf(checkQuantity(position));
        Log.i("StrQuantity", strQuantity);

        int quantity = Integer.parseInt(strQuantity);
        quantity = quantity + 1;
        if (quantity > data.getStok()) {
            viewHolder.btnAdd.setVisibility(View.GONE);
        } else {
            displayText(viewHolder, position, quantity);
        }
    }

    private void minusCart(ViewHolder viewHolder, JoinModel data, int position) {
        String strQuantity = String.valueOf(checkQuantity(position));
        Log.i("StrQuantity", strQuantity);
        int quantity = Integer.parseInt(strQuantity);
        quantity = quantity - 1;
        if (quantity <= 0) {
            viewHolder.layoutCart.setVisibility(View.GONE);
            viewHolder.beli.setVisibility(View.VISIBLE);
            deleteRowCart(position);
        } else {
            displayText(viewHolder, position, quantity);
        }
    }

    private void deleteRowCart(int position) {
        try{
            cartOperations.openDb();
            cartOperations.deleteRow(String.valueOf(items.get(position).getIdproduk()));
            Log.i("DeleteRow", "Masuk");
            cartOperations.closeDb();
        }catch (SQLException e){
            Log.i("DeleteRowError", e.getMessage());
        }
    }

    private void insertFirst(int position, int quantity) {
        Log.i("Position", String.valueOf(position));
        try {
            cartOperations.openDb();
            CartModel carts = new CartModel(items.get(position).getIdproduk(), quantity);
            cartOperations.insertCart(carts);
            cartOperations.closeDb();
            refreshView(position);
        } catch (SQLException e) {
            Log.i("UpdateSqlFirst", e.getMessage());
        }
    }

    public void refreshView(int position) {
        if (joinData()) {
            notifyItemChanged(position);
            //fragment.onResume();
        }
    }

    private Boolean joinData() {
        boolean join = false;
        try {
            shopOperations.openDb();
            shopOperations.joinData();
            //insert to adapter
            join = true;
            shopOperations.closeDb();
        } catch (SQLException e) {
            Log.d("ERROR JOIN", e.getMessage());
        }
        return false;
    }

    private int checkQuantity(int position) {
        int quantity = 0;
        try {
            shopOperations.openDb();
            quantity = shopOperations.joinData().get(position).getQuantity();
            Log.i("DataShop", String.valueOf(shopOperations.joinData().get(position).getQuantity()));
            shopOperations.closeDb();
        } catch (SQLException e) {
            Log.i("SqlException", e.getMessage());
        }
        Log.i("QuantityCart", String.valueOf(quantity));
        return quantity;
    }
    private void displayText(ViewHolder viewHolder, int position, int quantity) {
        updateQuantity(position, quantity);
        viewHolder.textQuantity.setText(String.valueOf(checkQuantity(position)));
        refreshView(position);
    }

    private void updateQuantity(int position, int quantity) {
        try {
            cartOperations.openDb();
            CartModel cartModel = new CartModel(items.get(position).getIdproduk(), quantity);
            cartOperations.updateCart(cartModel);
            cartOperations.closeDb();
            Log.i("SqlUpate", "masuk");
        } catch (SQLException e) {
            Log.i("ErrorSqlUpdate", e.getMessage());
        }
    }

    public void detailItem(JoinModel data) {
        gson = new Gson();
        String json = gson.toJson(data);
        Intent intent = new Intent(context, DetailShop.class);
        intent.putExtra("DATA", json);
        context.startActivity(intent);
    }

    public void clear(){
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView nama, harga, textQuantity, textStokNull, label;
        Button beli;
        CardView lihat;
        RelativeLayout layoutCart;
        ImageButton btnAdd, btnMinus;
        public ViewHolder(@NonNull View itemView,final OnItemClickListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgv_listItem_shop_listDetail);
            nama = itemView.findViewById(R.id.tv_namaBarangListItem_shop_listDetail);
            harga = itemView.findViewById(R.id.tv_hargaBarangListItem_shop_listDetail);
            beli = itemView.findViewById(R.id.btn_beliItemShopListDetail);
            lihat = itemView.findViewById(R.id.btnLihatListDetail);
            layoutCart = itemView.findViewById(R.id.layout_addToCart_adapterListDetail);
            btnAdd = itemView.findViewById(R.id.btn_addCart_adapter_ListDetail);
            btnMinus = itemView.findViewById(R.id.btn_minusCart_adapter_LIstDetail);
            textQuantity = itemView.findViewById(R.id.tv_jumlahBarang_adapter_ListDetail);
            textStokNull = itemView.findViewById(R.id.tvStokNullListDetail);
            label = itemView.findViewById(R.id.labelListItemListDetail);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemCickFilter(position, items.get(position));
                        }
                    }
                }
            });
        }
    }
}
