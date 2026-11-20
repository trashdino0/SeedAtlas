package dev.seedatlas.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * One observation in an evidence bundle.
 *
 * <p>Evidence is either localization evidence (it helps place a build/anchor) or
 * seed evidence (it constrains the world seed). The {@link #type()} discriminator
 * is written by Jackson's type-info mechanism.</p>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextureRotationEvidence.class, name = "texture_rotation"),
        @JsonSubTypes.Type(value = BiomeEvidence.class, name = "biome"),
        @JsonSubTypes.Type(value = StructureEvidence.class, name = "structure"),
        @JsonSubTypes.Type(value = StrongholdEvidence.class, name = "stronghold"),
        @JsonSubTypes.Type(value = SpawnEvidence.class, name = "spawn"),
        @JsonSubTypes.Type(value = EndPillarEvidence.class, name = "end_pillar"),
        @JsonSubTypes.Type(value = SeedHintEvidence.class, name = "seed_hint")
})
public sealed interface Evidence permits
        TextureRotationEvidence,
        BiomeEvidence,
        StructureEvidence,
        StrongholdEvidence,
        SpawnEvidence,
        EndPillarEvidence,
        SeedHintEvidence {

    String id();

    String source();

    double confidence();

    @JsonIgnore
    String type();
}
