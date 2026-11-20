package dev.seedatlas.nativebridge;

/**
 * @param salt       unsigned 32-bit structure salt, carried in a long
 * @param regionSize region size in chunks
 * @param chunkRange placement range in chunks
 * @param structType native Cubiomes structure type id
 */
public record StructureConfigInfo(long salt, int regionSize, int chunkRange, int structType) {
}
