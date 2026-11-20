package dev.seedatlas.core.validation;

import dev.seedatlas.core.analysis.DifficultyEstimator;
import dev.seedatlas.core.analysis.DifficultyReport;
import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.Constraints;
import dev.seedatlas.core.model.EndPillarEvidence;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.SeedHintEvidence;
import dev.seedatlas.core.model.SeedRange;
import dev.seedatlas.core.model.SpawnEvidence;
import dev.seedatlas.core.model.StrongholdEvidence;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import dev.seedatlas.core.version.VersionProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ProjectValidator {

    private static final Set<String> DIMENSIONS = Set.of("overworld", "nether", "end");
    private static final Set<String> EDITIONS = Set.of("java");

    private ProjectValidator() {
    }

    public static ValidationResult validate(Project project) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (project == null) {
            errors.add("Project is null.");
            return new ValidationResult(errors, warnings);
        }

        if (project.schemaVersion() != Project.CURRENT_SCHEMA_VERSION) {
            errors.add("Unsupported schemaVersion " + project.schemaVersion()
                    + "; expected " + Project.CURRENT_SCHEMA_VERSION + ".");
        }

        if (project.project() == null) {
            errors.add("project metadata is missing.");
        } else if (blank(project.project().name())) {
            warnings.add("project.name is blank; give the bundle a recognizable name.");
        }

        if (project.world() == null) {
            errors.add("world descriptor is missing.");
        } else {
            validateWorld(project.world(), errors, warnings);
        }

        Set<String> anchorIds = new HashSet<>();
        for (Anchor anchor : project.anchors()) {
            validateAnchor(anchor, anchorIds, errors, warnings);
        }

        Set<String> evidenceIds = new HashSet<>();
        for (Evidence evidence : project.evidence()) {
            validateEvidence(evidence, evidenceIds, anchorIds, errors, warnings);
        }

        validateConstraints(project.constraints(), errors, warnings);

        if (project.evidence().isEmpty()) {
            warnings.add("No evidence in the bundle; there is nothing to solve yet.");
        }

        DifficultyReport report = DifficultyEstimator.estimate(project);
        if (report.totalBits() < 24.0) {
            warnings.add("Estimated evidence is only " + String.format(Locale.ROOT, "%.1f", report.totalBits())
                    + " bits (" + report.tier() + "); add more structure or biome evidence.");
        }
        boolean hasStructureOrPillar = project.evidence().stream()
                .anyMatch(e -> e instanceof StructureEvidence || e instanceof EndPillarEvidence);
        if (!hasStructureOrPillar && !project.evidence().isEmpty()) {
            warnings.add("No structure or End pillar evidence; the practical solver routes will be limited.");
        }
        boolean hasTexture = project.evidence().stream().anyMatch(TextureRotationEvidence.class::isInstance);
        if (hasTexture && report.totalBits() < 1.0) {
            warnings.add("Texture rotations localize the build but do not constrain the seed; add seed evidence.");
        }

        return new ValidationResult(errors, warnings);
    }

    private static void validateWorld(WorldDescriptor world, List<String> errors, List<String> warnings) {
        String edition = world.edition() == null ? "" : world.edition().trim().toLowerCase(Locale.ROOT);
        if (!EDITIONS.contains(edition)) {
            errors.add("world.edition must be 'java' (Bedrock is not supported).");
        }

        String dimension = world.dimension() == null ? "" : world.dimension().trim().toLowerCase(Locale.ROOT);
        if (!DIMENSIONS.contains(dimension)) {
            errors.add("world.dimension must be one of " + DIMENSIONS + ".");
        }

        if (world.version() == null || world.version().isBlank()) {
            errors.add("world.version is required.");
        } else if (!VersionProfiles.isSupported(world.version())) {
            errors.add("Unsupported Minecraft version '" + world.version()
                    + "'. Supported: " + VersionProfiles.supportedIds() + ".");
        }

        if (world.largeBiomes()) {
            warnings.add("largeBiomes is enabled; structure/biome configs differ from default worldgen.");
        }
    }

    private static void validateAnchor(
            Anchor anchor,
            Set<String> anchorIds,
            List<String> errors,
            List<String> warnings
    ) {
        if (anchor == null) {
            errors.add("anchors contains a null entry.");
            return;
        }

        String id = anchor.id();
        if (blank(id)) {
            errors.add("Anchor id is blank.");
        } else if (!anchorIds.add(id)) {
            errors.add("Duplicate anchor id: " + id + ".");
        }

        String frame = anchor.frame();
        if (!"local".equals(frame) && !"world".equals(frame)) {
            errors.add("Anchor '" + id + "' has invalid frame '" + frame + "'; expected 'local' or 'world'.");
        }
        if ("world".equals(frame) && anchor.origin() == null) {
            errors.add("Anchor '" + id + "' is a world anchor but has no origin.");
        }
        if (anchor.origin() == null && anchor.searchArea() == null) {
            errors.add("Anchor '" + id + "' is unresolved and has no searchArea.");
        }

        if (anchor.searchArea() != null) {
            var area = anchor.searchArea();
            if (area.xMin() > area.xMax()) {
                errors.add("Anchor '" + id + "' searchArea has xMin > xMax.");
            }
            if (area.zMin() > area.zMax()) {
                errors.add("Anchor '" + id + "' searchArea has zMin > zMax.");
            }
            if (area.yMin() > area.yMax()) {
                errors.add("Anchor '" + id + "' searchArea has yMin > yMax.");
            }
            if (area.yMin() < -64 || area.yMax() > 512) {
                warnings.add("Anchor '" + id + "' searchArea y range is outside the usual world height.");
            }
            double volume = (double) (area.xMax() - area.xMin() + 1L)
                    * (double) (area.zMax() - area.zMin() + 1L)
                    * (double) (area.yMax() - area.yMin() + 1L);
            if (volume > 1.0e9) {
                warnings.add("Anchor '" + id + "' searchArea is very large ("
                        + DifficultyReport.formatCount(volume) + " blocks); localization may take a long time.");
            }
        }
    }

    private static void validateEvidence(
            Evidence evidence,
            Set<String> evidenceIds,
            Set<String> anchorIds,
            List<String> errors,
            List<String> warnings
    ) {
        if (evidence == null) {
            errors.add("evidence contains a null entry.");
            return;
        }

        String id = blank(evidence.id()) ? "<missing-id>" : evidence.id();
        if (blank(evidence.id())) {
            errors.add("An evidence entry is missing an id.");
        } else if (!evidenceIds.add(evidence.id())) {
            errors.add("Duplicate evidence id: " + evidence.id() + ".");
        }

        if (blank(evidence.source())) {
            warnings.add("Evidence '" + id + "' has no source; provenance will be incomplete.");
        }

        double confidence = evidence.confidence();
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            errors.add("Evidence '" + id + "' confidence must be between 0 and 1.");
        }

        switch (evidence) {
            case TextureRotationEvidence t -> validateTexture(t, id, anchorIds, errors, warnings);
            case BiomeEvidence b -> validateBiome(b, id, anchorIds, errors, warnings);
            case StructureEvidence s -> validateStructure(s, id, anchorIds, errors, warnings);
            case StrongholdEvidence s -> validateStronghold(s, id, errors, warnings);
            case SpawnEvidence s -> validateSpawn(s, id, errors, warnings);
            case EndPillarEvidence p -> validateEndPillar(p, id, errors);
            case SeedHintEvidence h -> validateSeedHint(h, id, errors, warnings);
        }
    }

    private static void validateTexture(
            TextureRotationEvidence t,
            String id,
            Set<String> anchorIds,
            List<String> errors,
            List<String> warnings
    ) {
        if (blank(t.anchor())) {
            errors.add("Texture evidence '" + id + "' has no anchor.");
        } else if (!anchorIds.contains(t.anchor())) {
            errors.add("Texture evidence '" + id + "' references unknown anchor '" + t.anchor() + "'.");
        }
        if (t.rotation() < 0) {
            errors.add("Texture evidence '" + id + "' rotation must be >= 0.");
        }
        if (t.rotation() > 3) {
            warnings.add("Texture evidence '" + id + "' rotation is > 3; expected 0-3 for tops/bottoms.");
        }
        if (t.side() && t.rotation() > 1) {
            warnings.add("Texture evidence '" + id + "' is a side texture but rotation is > 1.");
        }
        if (t.y() < -64 || t.y() > 512) {
            warnings.add("Texture evidence '" + id + "' y is outside the usual world height.");
        }
    }

    private static void validateBiome(
            BiomeEvidence b,
            String id,
            Set<String> anchorIds,
            List<String> errors,
            List<String> warnings
    ) {
        validateFrame(b.frame(), id, anchorIds, errors);
        if (blank(b.biome())) {
            errors.add("Biome evidence '" + id + "' has no biome name.");
        }
        if (b.y() < -64 || b.y() > 512) {
            warnings.add("Biome evidence '" + id + "' y is outside the usual world height.");
        }
    }

    private static void validateStructure(
            StructureEvidence s,
            String id,
            Set<String> anchorIds,
            List<String> errors,
            List<String> warnings
    ) {
        validateFrame(s.frame(), id, anchorIds, errors);
        if (blank(s.structure())) {
            errors.add("Structure evidence '" + id + "' has no structure type.");
        }
        if (s.tolerance() < 0) {
            errors.add("Structure evidence '" + id + "' tolerance must be >= 0.");
        } else if (s.tolerance() > 256) {
            warnings.add("Structure evidence '" + id + "' tolerance is very large ("
                    + s.tolerance() + " blocks); it contributes little information.");
        }
    }

    private static void validateStronghold(
            StrongholdEvidence s,
            String id,
            List<String> errors,
            List<String> warnings
    ) {
        if (s.ring() < 1 || s.ring() > 8) {
            errors.add("Stronghold evidence '" + id + "' ring must be between 1 and 8.");
        }
        if (s.tolerance() < 0) {
            errors.add("Stronghold evidence '" + id + "' tolerance must be >= 0.");
        } else if (s.tolerance() > 512) {
            warnings.add("Stronghold evidence '" + id + "' tolerance is very large ("
                    + s.tolerance() + " blocks).");
        }
    }

    private static void validateSpawn(
            SpawnEvidence s,
            String id,
            List<String> errors,
            List<String> warnings
    ) {
        if (s.tolerance() < 0) {
            errors.add("Spawn evidence '" + id + "' tolerance must be >= 0.");
        } else if (s.tolerance() > 512) {
            warnings.add("Spawn evidence '" + id + "' tolerance is very large ("
                    + s.tolerance() + " blocks).");
        }
    }

    private static void validateEndPillar(
            EndPillarEvidence p,
            String id,
            List<String> errors
    ) {
        if (p.heights().size() != 10) {
            errors.add("End pillar evidence '" + id + "' must contain exactly 10 heights.");
        }
        for (int i = 0; i < p.heights().size(); i++) {
            Integer height = p.heights().get(i);
            if (height == null || height < 0 || height > 255) {
                errors.add("End pillar evidence '" + id + "' height[" + i + "] must be 0-255.");
            }
        }
    }

    private static void validateSeedHint(
            SeedHintEvidence h,
            String id,
            List<String> errors,
            List<String> warnings
    ) {
        if (h.knownSeed() == null && h.hashedSeed() == null && h.seedPrefix() == null) {
            errors.add("Seed hint evidence '" + id + "' must contain knownSeed, hashedSeed, or seedPrefix.");
        }
        if (h.knownSeed() != null && (h.hashedSeed() != null || h.seedPrefix() != null)) {
            warnings.add("Seed hint evidence '" + id + "' has knownSeed; other hints are redundant.");
        }
    }

    private static void validateFrame(
            String frame,
            String id,
            Set<String> anchorIds,
            List<String> errors
    ) {
        if (blank(frame)) {
            errors.add("Evidence '" + id + "' has no frame.");
        } else if (!"world".equals(frame) && !anchorIds.contains(frame)) {
            errors.add("Evidence '" + id + "' references unknown frame '" + frame + "'.");
        }
    }

    private static void validateConstraints(
            Constraints constraints,
            List<String> errors,
            List<String> warnings
    ) {
        if (constraints == null) {
            return;
        }
        SeedRange range = constraints.seedRange();
        if (range != null && range.min() > range.max()) {
            errors.add("constraints.seedRange has min > max.");
        }
        if (range != null) {
            warnings.add("A seed range is set; results only cover that range.");
        }
        for (Long excluded : constraints.excludeSeeds()) {
            if (excluded == null) {
                errors.add("constraints.excludeSeeds contains null.");
            }
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
