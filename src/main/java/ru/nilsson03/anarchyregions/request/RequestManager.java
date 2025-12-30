package ru.nilsson03.anarchyregions.request;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;

public class RequestManager {

    private static final int DEFAULT_MAX_PER_PLAYER = 5;
    private static final int DEFAULT_MAX_TOTAL = 250;

    private final int maxRequests;
    private final int maxStoredInvites;
    private final LinkedHashMap<String, Request> invites;

    public RequestManager(ParameterFile configFile) {
        Integer configuredMaxPerPlayer = configFile.getValueAs("settings.request_max_per_player", Integer.class);
        Integer configuredMaxTotal = configFile.getValueAs("settings.request_max_total", Integer.class);

        this.maxRequests = configuredMaxPerPlayer != null ? configuredMaxPerPlayer : DEFAULT_MAX_PER_PLAYER;
        this.maxStoredInvites = configuredMaxTotal != null ? configuredMaxTotal : DEFAULT_MAX_TOTAL;

        this.invites = new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Request> eldest) {
                return maxStoredInvites > 0 && size() > maxStoredInvites;
            }
        };
    }


    /**
     * Находит запрос по получателю и региону
     * @param targetUuid получатель
     * @param regionId регион
     * @return найденный запрос
     */
    @Nullable
    public Request findRequest(UUID targetUuid, UUID regionId) {
        return invites.get(generateKey(targetUuid, regionId));
    }

    /**
     * Удаляет запрос
     * @param request
     */
    public void removeRequest(Request request) {
        if (request == null) {
            return;
        }
        invites.remove(generateKey(request.target(), request.regionId()));
    }

    /**
     * Добавляет запрос
     * @param inviter пригласитель
     * @param target получатель
     * @param regionId идентификатор региона
     */
    public void addRequest(Player inviter, Player target, UUID regionId) {
        UUID inviterUuid = inviter.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        Request request = new Request(inviterUuid, targetUuid, regionId);
        invites.put(generateKey(targetUuid, regionId), request);
    }

    /**
     * Проверяет, можно ли отправить запрос
     * @param playerUuid игрок
     * @return можно ли отправить запрос
     */
    public boolean canSendInvite(UUID playerUuid) {
        long countRequests = getPlayerRequestsCount(playerUuid);
        return countRequests < maxRequests;
    }

    /**
     * Получает количество запросов игрока
     * @param playerUuid игрок
     * @return количество запросов
     */
    private long getPlayerRequestsCount(UUID playerUuid) {
        return invites.values().stream()
                .filter(invite -> invite.inviter().equals(playerUuid))
                .count();
    }

    private String generateKey(UUID targetUuid, UUID regionId) {
        return targetUuid.toString() + ":" + regionId.toString();
    }
}
