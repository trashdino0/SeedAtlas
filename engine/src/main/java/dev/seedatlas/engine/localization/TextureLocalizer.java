package dev.seedatlas.engine.localization;

import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.Position;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.SearchArea;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.texture.TextureProvider;
import dev.seedatlas.core.texture.TextureProviders;
import dev.seedatlas.core.version.VersionProfile;
import dev.seedatlas.core.version.VersionProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Brute-force localization of an anchor from texture-rotation evidence.
 *
 * <p>The rotation hash is seed-independent, so this is a pure coordinate search.
 * Top/bottom evidence (mod 4) is checked before side evidence (mod 2) because it
 * rejects more positions per check.</p>
 */
public final class TextureLocalizer {

    private TextureLocalizer() {
    }

    public static LocalizationResult localize(Project project, Anchor anchor, int threads) {
        if (project.world() == null) {
            throw new IllegalArgumentException("Project has no world descriptor.");
        }
        VersionProfile profile = VersionProfiles.find(project.world().version())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported Minecraft version: " + project.world().version()));
        TextureProvider provider = TextureProviders.forMode(profile.textureMode());

        List<TextureRotationEvidence> evidence = project.evidence().stream()
                .filter(TextureRotationEvidence.class::isInstance)
                .map(TextureRotationEvidence.class::cast)
                .filter(e -> anchor.id().equals(e.anchor()))
                .sorted(Comparator.comparing(TextureRotationEvidence::side))
                .toList();

        if (evidence.isEmpty()) {
            return new LocalizationResult(anchor.id(), List.of(), 0L, 0L);
        }

        long started = System.currentTimeMillis();

        if (anchor.origin() != null) {
            boolean matched = matches(provider, anchor.origin(), evidence);
            List<LocalizationMatch> matches = matched
                    ? List.of(new LocalizationMatch(anchor.origin(), evidence.size(), evidence.size()))
                    : List.of();
            return new LocalizationResult(
                    anchor.id(),
                    matches,
                    1L,
                    System.currentTimeMillis() - started
            );
        }

        SearchArea area = anchor.searchArea();
        if (area == null) {
            throw new IllegalArgumentException("Anchor '" + anchor.id() + "' has no origin and no searchArea.");
        }

        int workerCount = threads > 0
                ? threads
                : Math.max(1, Runtime.getRuntime().availableProcessors());

        int totalX = area.xMax() - area.xMin() + 1;
        int chunk = Math.max(1, (totalX + workerCount - 1) / workerCount);

        List<Callable<List<LocalizationMatch>>> tasks = new ArrayList<>();
        for (int startX = area.xMin(); startX <= area.xMax(); startX += chunk) {
            int fromX = startX;
            int toX = Math.min(area.xMax(), startX + chunk - 1);
            tasks.add(() -> scanRange(provider, evidence, fromX, toX, area));
        }

        List<LocalizationMatch> matches = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(workerCount)) {
            List<Future<List<LocalizationMatch>>> futures = pool.invokeAll(tasks);
            for (Future<List<LocalizationMatch>> future : futures) {
                matches.addAll(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Localization interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Localization failed: " + e.getCause().getMessage(), e.getCause());
        }

        matches.sort(Comparator
                .comparingInt((LocalizationMatch m) -> m.position().x())
                .thenComparingInt(m -> m.position().z())
                .thenComparingInt(m -> m.position().y()));

        long checked = (long) totalX
                * (area.zMax() - area.zMin() + 1L)
                * (area.yMax() - area.yMin() + 1L);

        return new LocalizationResult(
                anchor.id(),
                matches,
                checked,
                System.currentTimeMillis() - started
        );
    }

    private static List<LocalizationMatch> scanRange(
            TextureProvider provider,
            List<TextureRotationEvidence> evidence,
            int fromX,
            int toX,
            SearchArea area
    ) {
        List<LocalizationMatch> matches = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            for (int z = area.zMin(); z <= area.zMax(); z++) {
                for (int y = area.yMin(); y <= area.yMax(); y++) {
                    Position origin = new Position(x, y, z);
                    if (matches(provider, origin, evidence)) {
                        matches.add(new LocalizationMatch(origin, evidence.size(), evidence.size()));
                    }
                }
            }
        }
        return matches;
    }

    private static boolean matches(
            TextureProvider provider,
            Position origin,
            List<TextureRotationEvidence> evidence
    ) {
        for (TextureRotationEvidence e : evidence) {
            int mod = e.side() ? 2 : 4;
            int expected = e.side() ? (e.rotation() & 1) : e.rotation();
            int actual = provider.getTexture(
                    origin.x() + e.x(),
                    origin.y() + e.y(),
                    origin.z() + e.z(),
                    mod
            );
            if (actual != expected) {
                return false;
            }
        }
        return true;
    }
}
