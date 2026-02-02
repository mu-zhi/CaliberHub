package com.caliberhub.infrastructure.scene.supportimpl;

import com.caliberhub.domain.scene.model.Scene;
import com.caliberhub.domain.scene.support.SceneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景仓储实现（内存 Mock）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SceneRepositoryImpl implements SceneRepository {

    // Mock 存储
    private static final Map<String, Scene> SCENE_STORE = new ConcurrentHashMap<>();
    private static final Map<String, Scene> SCENE_CODE_INDEX = new ConcurrentHashMap<>();

    @Override
    public Optional<Scene> findById(String id) {
        return Optional.ofNullable(SCENE_STORE.get(id));
    }

    @Override
    public Optional<Scene> findBySceneCode(String sceneCode) {
        return Optional.ofNullable(SCENE_CODE_INDEX.get(sceneCode));
    }

    @Override
    public List<Scene> findByDomainId(String domainId) {
        return SCENE_STORE.values().stream()
                .filter(s -> domainId.equals(s.getDomainId()))
                .toList();
    }

    @Override
    public List<Scene> findAllActive() {
        return SCENE_STORE.values().stream()
                .filter(s -> s.getLifecycleStatus() == com.caliberhub.domain.scene.valueobject.SceneStatus.ACTIVE)
                .toList();
    }

    @Override
    public void save(Scene scene) {
        SCENE_STORE.put(scene.getId(), scene);
        SCENE_CODE_INDEX.put(scene.getSceneCode(), scene);
        log.info("Saved scene: {} ({})", scene.getSceneCode(), scene.getId());
    }

    @Override
    public void delete(String id) {
        Scene scene = SCENE_STORE.remove(id);
        if (scene != null) {
            SCENE_CODE_INDEX.remove(scene.getSceneCode());
            log.info("Deleted scene: {}", id);
        }
    }

    @Override
    public boolean existsBySceneCode(String sceneCode) {
        return SCENE_CODE_INDEX.containsKey(sceneCode);
    }
}
