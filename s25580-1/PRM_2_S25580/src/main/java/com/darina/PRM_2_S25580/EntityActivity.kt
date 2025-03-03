import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darina.PRM_2_S25580.R
import com.darina.PRM_2_S25580.adapter.EntityAdapter
import com.darina.PRM_2_S25580.db.DB
import com.darina.PRM_2_S25580.rep.EntityRep
import com.darina.PRM_2_S25580.view_model.EntityViewModel
import com.darina.PRM_2_S25580.view_model.EntityViewModelStorage

class EntityActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var entityAdapter: EntityAdapter
    private lateinit var entityViewModel: EntityViewModel
    private lateinit var itemCountTextView: TextView
    private lateinit var buttonGoToMainActivity: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entities)

        setupToolbar()
        setupRecyclerView()
        setupViewModel()
        setupButton()

        entityViewModel.allEntities.observe(this) { entities ->
            entities?.let {
                entityAdapter.updateData(it)
                updateItemCount(it.size)
            }
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
    }

    private fun setupViewModel() {
        val entityDao = DB.getIstance(application).entityDao()
        val repository = EntityRep(entityDao)
        val viewModelFactory = EntityViewModelStorage(repository)
        entityViewModel = ViewModelProvider(this, viewModelFactory).get(EntityViewModel::class.java)
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        entityAdapter = EntityAdapter(this,mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = entityAdapter
    }

    private fun updateItemCount(count: Int) {
        itemCountTextView = findViewById(R.id.itemCountTextView)
        itemCountTextView.text = "Entries count: $count"
    }

    private fun setupButton() {
        buttonGoToMainActivity = findViewById(R.id.buttonGoToMainActivity)
        buttonGoToMainActivity.setOnClickListener {
            startActivity(Intent(this, Creation::class.java))
        }
    }
}
