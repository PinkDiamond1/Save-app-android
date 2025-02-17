package net.opendasharchive.openarchive.features.media.grid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Collection.Companion.getAllAsList
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.Media.Companion.getMediaByProjectAndCollection
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.features.media.SectionViewHolder
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListActivity

class MediaGridFragment : MediaListFragment() {

    private val numberOfColumns = 4
    private var mAdapters = HashMap<Long, MediaAdapter>()
    private var mSection = HashMap<Long, SectionViewHolder>()

    private var _mBinding: FragmentMediaListBinding? = null
    private lateinit var viewModel: MediaGridViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MediaGridViewModel::class.java)
        observeData()
        viewModel.getAllCollection()
        return _mBinding?.root
    }

    private fun observeData() {

        viewModel.collections.observe(viewLifecycleOwner, Observer {
            initLayout(it)
        })

    }

    private fun initLayout(listCollections: List<Collection>?) {
        mAdapters = HashMap()
        mSection = HashMap()

        _mBinding?.let { mBinding ->
            mBinding.root.tag = TAG

            var addedView = false
            listCollections?.forEach { collection ->
                val listMedia = getMediaByProjectAndCollection(getProjectId(), collection.id)
                if (!listMedia.isNullOrEmpty()) {
                    if (!addedView) {
                        mBinding.mediacontainer.removeAllViews()
                        addedView = true
                    }

                    val view: View? = createMediaList(collection, listMedia)
                    mBinding.mediacontainer.addView(view)
                }
            }
            mBinding.addMediaHint.visibility = if (addedView) View.GONE else View.VISIBLE
        }
    }

    private fun createMediaList(collection: Collection, listMedia: List<Media>): View? {

        val holder = SectionViewHolder()

        holder.apply {
            mediaSection = layoutInflater.inflate(R.layout.fragment_media_list_section, null)
            val rView: RecyclerView? = mediaSection?.findViewById(R.id.recyclerview)
            if (rView != null) {
                rView.setHasFixedSize(true)
                rView.layoutManager = GridLayoutManager(activity, numberOfColumns)
                sectionStatus = mediaSection?.findViewById(R.id.sectionstatus)
                sectionTimestamp = mediaSection?.findViewById(R.id.sectiontimestamp)
                action = mediaSection?.findViewById(R.id.action_next)
                setSectionHeaders(collection, listMedia, this)
                val listMediaArray = ArrayList(listMedia)
                val mediaAdapter = MediaAdapter(
                    requireContext(),
                    R.layout.activity_media_list_square,
                    listMediaArray,
                    rView,
                    object : OnStartDragListener {
                        override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {

                        }
                    }, onDelete = {
                        refresh()
                    })
                rView.adapter = mediaAdapter
                mAdapters[collection.id] = mediaAdapter
                mSection[collection.id] = this
            }
        }
        return holder.mediaSection
    }

    override fun updateItem(mediaId: Long, progress: Long) {
        for (adapter in mAdapters.values) adapter.updateItem(mediaId, progress)
    }

    override fun refresh() {
        val listCollections = getAllAsList()
        listCollections?.forEach { collection ->
            val listMedia = getMediaByProjectAndCollection(getProjectId(), collection.id)
            val adapter = mAdapters[collection.id]
            val holder: SectionViewHolder? = mSection[collection.id]
            val listMediaArray = ArrayList(listMedia)
            if (adapter != null) {
                adapter.updateData(listMediaArray)
                setSectionHeaders(collection, listMedia, holder)
            } else if (!listMedia.isNullOrEmpty()) {
                val view = createMediaList(collection, listMedia)
                _mBinding?.let { mBinding ->
                    mBinding.mediacontainer.addView(view, 0)
                    mBinding.addMediaHint.visibility = View.GONE
                }
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSectionHeaders(
        collection: Collection,
        listMedia: List<Media>?,
        holder: SectionViewHolder?
    ) {

        holder?.sectionStatus?.text = ""
        holder?.sectionTimestamp?.text = ""

        listMedia?.forEach { media ->
            when (media.status) {
                Media.STATUS_LOCAL -> {
                    holder?.let {
                        holder.sectionStatus?.text = getString(R.string.status_ready_to_upload)
                        holder.sectionTimestamp?.text = "${listMedia.size} ${getString(R.string.label_items)}"
                        holder.action?.visibility = View.INVISIBLE
                        holder.action?.setOnClickListener {
                            startActivity(
                                Intent(
                                    requireActivity(),
                                    PreviewMediaListActivity::class.java
                                )
                            )
                        }
                    }
                    return@forEach
                }
                Media.STATUS_QUEUED, Media.STATUS_UPLOADING -> {
                    holder?.let {
                        holder.sectionStatus?.text = getString(R.string.header_uploading)
                        var uploadedCount = 0
                        listMedia.forEach { localMedia ->if (localMedia.status == Media.STATUS_UPLOADED) uploadedCount++ }
                        holder.sectionTimestamp?.text = uploadedCount.toString() + " " + getString(R.string.label_out_of) + " " + listMedia.size + ' ' + getString(R.string.label_items_uploaded)
                        holder.action?.visibility = View.INVISIBLE
                    }
                    return@forEach
                }
                Media.STATUS_UPLOADED -> {
                    holder?.let {
                        var uploadedCount = 0
                        listMedia.forEach { localMedia -> if (localMedia.status == Media.STATUS_UPLOADED) uploadedCount++ }
                        if (uploadedCount == listMedia.size) {
                            holder.sectionStatus?.text = listMedia.size.toString() + " " + getString(R.string.label_items_uploaded)
                            holder.action?.visibility = View.INVISIBLE
                        } else {
                            holder.sectionStatus?.text = uploadedCount.toString() + " " + getString(R.string.label_out_of) + " " + listMedia.size + ' ' + getString(R.string.label_items_uploaded)
                            holder.action?.visibility = View.INVISIBLE
                        }
                        if (collection.uploadDate != null) holder.sectionTimestamp?.text = collection.uploadDate?.toLocaleString()
                    }
                }
                Media.STATUS_ERROR -> {
                    holder?.let {
                        var uploadedCount = 0
                        listMedia.forEach { localMedia -> if (localMedia.status == Media.STATUS_ERROR) uploadedCount++ }
                        if (uploadedCount == listMedia.size) {
                            holder.sectionStatus?.text = listMedia.size.toString() + " " + getString(R.string.label_items_remaining)
                        } else {
                            holder.sectionStatus?.text = uploadedCount.toString() + " " + getString(R.string.label_out_of) + " " + listMedia.size + ' ' + getString(R.string.label_items_remaining)
                        }
                        if (collection.uploadDate != null) holder.sectionTimestamp?.text = collection.uploadDate?.toLocaleString()
                        holder.action?.visibility = View.INVISIBLE
                    }
                }
                else -> {
                    holder?.let {
                        holder.sectionStatus?.text = listMedia.size.toString() + " " + getString(R.string.label_items_uploaded)
                        if (collection.uploadDate != null) holder.sectionTimestamp?.text = collection.uploadDate?.toLocaleString()
                        else if (listMedia.isNotEmpty() && listMedia[0].uploadDate != null) holder.sectionTimestamp?.text = listMedia[0].uploadDate.toString()
                        holder.action?.visibility = View.INVISIBLE
                    }
                }
            }

        }
    }

}