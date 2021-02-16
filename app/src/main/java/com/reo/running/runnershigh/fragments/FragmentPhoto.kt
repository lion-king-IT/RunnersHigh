package com.reo.running.runnershigh.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reo.running.runnershigh.MyAdapter2
import com.reo.running.runnershigh.MyApplication
import com.reo.running.runnershigh.Record2
import com.reo.running.runnershigh.databinding.FragmentPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentPhoto : Fragment() {
    private lateinit var binding:FragmentPhotoBinding
    private val readDao = MyApplication.db.recordDao2()
    val photo = listOf<Record2>()
    private var i = 0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPhotoBinding.inflate(layoutInflater,container,false)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.run {
            lifecycleScope.launch(Dispatchers.IO) {
//                val db = readDao.getAll2

                withContext(Dispatchers.Main) {

                }
            }
//            mainRecyclerView.adapter = MyAdapter2(photo)
            mainRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
    }
}