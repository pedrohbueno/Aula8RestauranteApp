package edu.unifaj.aula8restauranteapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.1.104:8080/restaurante/mesa/";
    private Spinner spinnerMesa;
    private EditText etResponsavel;
    private EditText etProduto;
    private EditText etQuantidade;
    private EditText etValor;
    private ListView lvConsumo;
    private TextView tvTotal;
    private Button btnAdd, btnAbrir, btnAtualizar, btnFechar;

    private ArrayList<String> listaConsumo = new ArrayList<>();
    private ArrayAdapter<String> adapterConsumo;
    private JSONArray pedidosArray = new JSONArray();
    private double total = 0;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa Volley
        requestQueue = Volley.newRequestQueue(this);

        // Referencia as views
        spinnerMesa   = findViewById(R.id.spinnerMesa);
        etResponsavel = findViewById(R.id.etResponsavel);
        etProduto     = findViewById(R.id.etProduto);
        etQuantidade  = findViewById(R.id.etQuantidade);
        etValor       = findViewById(R.id.etValor);
        lvConsumo     = findViewById(R.id.lvConsumo);
        tvTotal       = findViewById(R.id.tvTotal);
        btnAdd        = findViewById(R.id.btnAdd);
        btnAbrir      = findViewById(R.id.btnAbrir);
        btnAtualizar  = findViewById(R.id.btnAtualizar);
        btnFechar     = findViewById(R.id.btnFechar);

        // Configura Spinner de Mesas (1 a 6)
        String[] mesas = {"1", "2", "3", "4", "5", "6"};
        ArrayAdapter<String> adapterMesa = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, mesas);
        adapterMesa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMesa.setAdapter(adapterMesa);

        // Configura ListView de consumo
        adapterConsumo = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, listaConsumo);
        lvConsumo.setAdapter(adapterConsumo);

        // Botão Adicionar - adiciona item na lista local
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adicionarItem();
            }
        });

        // Botão Abrir - PUT para enviar pedido ao servidor
        btnAbrir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirMesa();
            }
        });

        // Botão Atualizar - GET para buscar informações
        btnAtualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                atualizarMesa();
            }
        });

        // Botão Fechar - DELETE para fechar a mesa
        btnFechar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fecharMesa();
            }
        });

    }
    // Adicionar - Adiciona nova linha na ListView com Produto - Quantidade - Valor
    private void adicionarItem() {
        String produto    = etProduto.getText().toString().trim();
        String quantidade = etQuantidade.getText().toString().trim();
        String valorStr   = etValor.getText().toString().trim();

        if (produto.isEmpty() || quantidade.isEmpty() || valorStr.isEmpty()) {
            Toast.makeText(this, "Preencha Produto, Quantidade e Valor!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int qtd    = Integer.parseInt(quantidade);
            double val = Double.parseDouble(valorStr);

            // Adiciona na lista visual
            String linha = produto + "  |  " + qtd + "  |  R$ " + String.format("%.2f", val);
            listaConsumo.add(linha);
            adapterConsumo.notifyDataSetChanged();

            // Acumula total
            total += val;
            tvTotal.setText("Total: R$ " + String.format("%.2f", total));

            // Monta objeto JSON do item
            JSONObject item = new JSONObject();
            item.put("produto", produto);
            item.put("quantidade", qtd);
            item.put("valor", val);
            pedidosArray.put(item);

            // Limpa campos
            etProduto.setText("");
            etQuantidade.setText("");
            etValor.setText("");

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantidade e Valor devem ser numéricos!", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.fillInStackTrace();
        }
    }

    // Abrir - PUT para enviar informações ao Servidor
    private void abrirMesa() {
        String idMesa      = spinnerMesa.getSelectedItem().toString();
        String responsavel = etResponsavel.getText().toString().trim();

        if (responsavel.isEmpty()) {
            Toast.makeText(this, "Informe o nome do Responsável (cliente)!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pedidosArray.length() == 0) {
            Toast.makeText(this, "Adicione ao menos um item antes de abrir a mesa!", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + idMesa;

        try {
            JSONObject body = new JSONObject();
            body.put("idMesa", Integer.parseInt(idMesa));
            body.put("responsavel", responsavel);
            body.put("pedidos", pedidosArray);
            body.put("total", total);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT, url, body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Toast.makeText(MainActivity.this,
                                    "Mesa aberta com sucesso!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(MainActivity.this,
                                    "Erro ao abrir mesa: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();

                        }
                    });

            requestQueue.add(request);

        } catch (JSONException e) {
            e.fillInStackTrace();
        }
    }

    // Atualizar - GET para buscar as informações da mesa
    private void atualizarMesa() {
        String idMesa = spinnerMesa.getSelectedItem().toString();
        String url    = BASE_URL + idMesa;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Limpa lista atual
                            listaConsumo.clear();
                            pedidosArray = new JSONArray();
                            total = 0;

                            // Preenche responsável
                            if (response.has("responsavel")) {
                                etResponsavel.setText(response.getString("responsavel"));
                            }

                            // Preenche pedidos
                            if (response.has("pedidos")) {
                                JSONArray pedidos = response.getJSONArray("pedidos");
                                pedidosArray = pedidos;
                                for (int i = 0; i < pedidos.length(); i++) {
                                    JSONObject item = pedidos.getJSONObject(i);
                                    String produto  = item.getString("produto");
                                    int qtd         = item.getInt("quantidade");
                                    double val      = item.getDouble("valor");
                                    total += val;
                                    listaConsumo.add(produto + "  |  " + qtd + "  |  R$ " + String.format("%.2f", val));
                                }
                            }

                            // Atualiza total
                            if (response.has("total")) {
                                total = response.getDouble("total");
                            }
                            tvTotal.setText("Total: R$ " + String.format("%.2f", total));
                            adapterConsumo.notifyDataSetChanged();

                            Toast.makeText(MainActivity.this, "Mesa atualizada!", Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            e.fillInStackTrace();
                            Toast.makeText(MainActivity.this,
                                    "Erro ao processar resposta!", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao consultar mesa: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

        requestQueue.add(request);
    }
    // Fechar - DELETE para fechar a mesa
    private void fecharMesa() {
        String idMesa = spinnerMesa.getSelectedItem().toString();
        String url    = BASE_URL + idMesa;

        StringRequest request = new StringRequest(
                Request.Method.DELETE, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Limpa a tela
                        listaConsumo.clear();
                        pedidosArray = new JSONArray();
                        total = 0;
                        etResponsavel.setText("");
                        tvTotal.setText("Total: R$ 0,00");
                        adapterConsumo.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this,
                                "Mesa fechada com sucesso!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this,
                                "Erro ao fechar mesa: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

        requestQueue.add(request);
    }


}