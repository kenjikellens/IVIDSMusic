import { Song } from './Song.js';
import { Album } from './Album.js';
import { Artist } from './Artist.js';

/**
 * Data Transfer Object (DTO) Mapper for transforming raw API responses
 * into standardized Domain Entities.
 */
export class DTOMapper {
    /**
     * Maps Deezer/iTunes raw song objects into a Song domain entity.
     * @param {Object} raw
     * @returns {Song} Domain entity
     */
    static toSong(raw = {}) {
        if (raw instanceof Song) return raw;
        return new Song({
            id: raw.id || raw.trackId,
            title: raw.title || raw.trackName,
            artist: raw.artist?.name || raw.artistName || raw.artist || 'Unknown Artist',
            artistId: raw.artist?.id || raw.artistId || null,
            album: raw.album?.title || raw.collectionName || raw.album || '',
            albumId: raw.album?.id || raw.albumId || null,
            cover: raw.album?.cover_medium || raw.album?.cover_big || raw.cover || raw.artworkUrl100,
            duration: raw.duration || Math.floor((raw.trackTimeMillis || 0) / 1000),
            previewUrl: raw.preview || raw.previewUrl || ''
        });
    }

    /**
     * Maps Deezer/iTunes raw album objects into an Album domain entity.
     * @param {Object} raw
     * @returns {Album} Domain entity
     */
    static toAlbum(raw = {}) {
        if (raw instanceof Album) return raw;
        return new Album({
            id: raw.id || raw.collectionId,
            title: raw.title || raw.collectionName,
            artist: raw.artist?.name || raw.artistName || raw.artist || 'Unknown Artist',
            artistId: raw.artist?.id || raw.artistId || null,
            cover: raw.cover_medium || raw.cover_big || raw.cover || raw.artworkUrl100,
            trackCount: raw.nb_tracks || raw.trackCount || 0,
            releaseYear: raw.release_date ? raw.release_date.substring(0, 4) : ''
        });
    }

    /**
     * Maps Deezer/iTunes raw artist objects into an Artist domain entity.
     * @param {Object} raw
     * @returns {Artist} Domain entity
     */
    static toArtist(raw = {}) {
        if (raw instanceof Artist) return raw;
        return new Artist({
            id: raw.id || raw.artistId,
            name: raw.name || raw.artistName,
            cover: raw.picture_medium || raw.picture_big || raw.cover || raw.artworkUrl100,
            genre: raw.genre || 'Artist',
            fanCount: raw.nb_fan || raw.fanCount || 0
        });
    }

    /**
     * Maps an array of raw objects based on item type.
     * @param {Array<Object>} list
     * @param {string} type
     * @returns {Array<Song|Album|Artist>} Domain entities list
     */
    static mapList(list = [], type = 'song') {
        if (!Array.isArray(list)) return [];
        return list.map(item => {
            const itemType = item.type || type;
            if (itemType === 'artist') return DTOMapper.toArtist(item);
            if (itemType === 'album') return DTOMapper.toAlbum(item);
            return DTOMapper.toSong(item);
        });
    }
}
