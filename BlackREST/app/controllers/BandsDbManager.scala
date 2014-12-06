package controllers

import collection.immutable.Map

object BandsDbManager {
  type Year = Int
  type BandName = String
  type AlbumName = String
  type AlbumEntry = (Year, AlbumName)

  private object BandsDb {
    private def bands = Map[BandName, List[AlbumEntry]](
      "behemoth" ->
        ((1995, "Sventevith") ::
          (1996, "Grom") ::
          (1998, "Pandemonic Incantations") ::
          (1999, "Satanica") ::
          (2000, "Thelema.6") ::
          (2002, "Zos Kia Cultus (Here and Beyond)") ::
          (2004, "Demigod") ::
          (2007, "The Apostasy") ::
          (2009, "Evangelion") ::
          (2014, "The Satanist") ::
          Nil),
      "emperor" ->
        ((1994, "In the Nightside Eclipse") ::
          (1997, "Anthems to the Welkin at Dusk") ::
          (1999, "IX Equilibrium") ::
          (2001, "Prometheus: The Discipline of Fire & Demise") ::
          Nil),
      "burzum" ->
        ((1992, "Burzum") ::
          (1993, "Det som engang var") ::
          (1994, "Hvis lyset tar oss") ::
          (1996, "Filosofem") ::
          (1997, "Dauði Baldrs") ::
          (1999, "Hliðskjálf") ::
          (2010, "Belus") ::
          (2011, "Fallen") ::
          (2012, "Umskiptar") ::
          (2013, "Sôl austan, Mâni vestan") ::
          (2014, "The Ways of Yore") ::
          Nil),
      "immortal" ->
        ((1992, "Diabolical Fullmoon Mysticism") ::
          (1993, "Pure Holocaust") ::
          (1995, "Battles in the North") ::
          (1997, "Blizzard Beasts") ::
          (1999, "At the Heart of Winter") ::
          (2000, "Damned in Black") ::
          (2002, "Sons of Northern Darkness") ::
          (2009, "All Shall Fall") ::
          Nil)
    )

    def getAlbums(bandName: BandName): Option[List[AlbumEntry]] = bands get bandName

  }

  type TaskId = Int

  private val bandsDb = BandsDb

  private var resultsCache = Map[TaskId, Option[(BandName, List[AlbumEntry])]]()
  private var lastTaskId: TaskId = 0

  def get(id: TaskId): Option[Option[(BandName, List[AlbumEntry])]] = resultsCache get id

  def put(bandName: BandName, startYear: Year): TaskId = {
    val reqRes = albumsBeginningFrom(bandName.toLowerCase, startYear)
    lastTaskId += 1
    resultsCache = resultsCache + (lastTaskId -> reqRes)
    lastTaskId
  }

  def post(taskId: TaskId, newStartYear: Year): Boolean = {
    val mbBandName =
      resultsCache get taskId flatMap {mbCached => mbCached flatMap {cached => Some(cached._1)}}
    mbBandName match {
      case Some(bandName) => {
        resultsCache = resultsCache updated(taskId, albumsBeginningFrom(bandName, newStartYear))
        true
      }
      case _              => false
    }
  }

  def delete(taskId: TaskId): Boolean = {
    resultsCache contains taskId match {
      case true =>
        resultsCache = resultsCache - taskId
        true
      case false => false
    }
  }

  private def albumsBeginningFrom(bandName: BandName, year: Year): Option[(BandName, List[AlbumEntry])] =
    bandsDb getAlbums bandName flatMap {x => Some(bandName, x filter { pair => pair._1 >= year })}
}