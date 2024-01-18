package mil.nga.giat.mage.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mil.nga.giat.mage.search.GeocoderResult
import mil.nga.giat.mage.R
import mil.nga.giat.mage.coordinate.CoordinateFormatter

class SearchResultsListAdapter(
   private val context: Context,
   private val searchResults: List<GeocoderResult>,
   private val listener: OnSearchResultClickListener
) : RecyclerView.Adapter<SearchResultsListAdapter.ViewHolder>() {

   interface OnSearchResultClickListener {
      fun onSearchResultClick(result: GeocoderResult)
   }

   class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val name: TextView
      val address: TextView
      val location: TextView

      init {
         name = view.findViewById(R.id.name)
         address = view.findViewById(R.id.address)
         location = view.findViewById(R.id.location)
      }
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.geocoder_search_result_list_item, parent, false)
      return ViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val result = searchResults[position]

      holder.name.text = result.name
      holder.address.text = result.address
      holder.location.text = CoordinateFormatter(context).format(result.location)

      holder.itemView.setOnClickListener {
         listener.onSearchResultClick(result)
      }
   }

   override fun getItemCount(): Int {
      return searchResults.size
   }
}