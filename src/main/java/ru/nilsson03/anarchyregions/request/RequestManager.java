package ru.nilsson03.anarchyregions.request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import ru.nilsson03.library.bukkit.file.configuration.ParameterFile;

public class RequestManager {

    private final int maxRequests;
    private final List<Request> invites = new ArrayList<>();

    public RequestManager(ParameterFile configFile) {
        this.maxRequests = configFile.getValueAs("settings.request_max_per_player", Integer.class);
    }


    /**
     * Находит запрос по получателю и региону
     * @param targetUuid получатель
     * @param regionId регион
     * @return найденный запрос
     */
    @Nullable
    public Request findRequest(UUID targetUuid, UUID regionId) {
        return invites.stream()
                .filter(invite -> invite.target().equals(targetUuid)
                        && invite.regionId().equals(regionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Удаляет запрос
     * @param request
     */
    public void removeRequest(Request request) {
        invites.remove(request);
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
        invites.add(request);
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
        return invites.stream().filter(invite -> invite.inviter().equals(playerUuid))
                .count();
    }
}
