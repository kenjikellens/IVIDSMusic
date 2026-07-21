# Object-Oriented Domain Modeling Rules

Every developer and AI agent working on IVIDS Music MUST strictly adhere to these Object-Oriented Design (OOP) rules:

1. **First-Class Domain Entities**: Core domain concepts like `Song`, `Album`, and `Artist` must be first-class, standalone object entities in the domain layer (`com.kenjigames.ividsmusic.domain.model`).
2. **Typed Object Relationships**: Domain entities must maintain rich object references (e.g. `Song` references its `artist: Artist?` and `album: Album?`; `Album` references its `artist: Artist?` and list of `songs: List<Song>`) rather than relying solely on flat un-typed string primitives.
3. **Dedicated DTO Mappers**: Every API endpoint and data source must have dedicated Data Transfer Object (DTO) mappers for converting raw network/database responses directly into domain entities.
4. **Isolated API Endpoints**: Use specific API endpoints (e.g., `search`, `search/album`, `search/artist`) to retrieve full entity metadata rather than inferring entities from partial flat properties.
