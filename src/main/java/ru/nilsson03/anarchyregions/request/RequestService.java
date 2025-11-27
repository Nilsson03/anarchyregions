package ru.nilsson03.anarchyregions.request;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import ru.nilsson03.anarchyregions.event.RequestAcceptedEvent;
import ru.nilsson03.anarchyregions.region.Region;
import ru.nilsson03.anarchyregions.region.manager.RegionManager;
import ru.nilsson03.library.bukkit.file.configuration.BukkitConfig;
import ru.nilsson03.library.text.component.ClickableMessage;
import ru.nilsson03.library.text.component.action.impl.RunnableAction;
import ru.nilsson03.library.text.messeger.UniversalMessenger;
import ru.nilsson03.library.text.util.ReplaceData;

@AllArgsConstructor
public class RequestService {

    private final RegionManager regionManager;
    private final RequestManager requestManager;
    private final BukkitConfig messagesConfig;

    public enum InviteResult {
        SUCCESS,
        REGION_NOT_FOUND,
        NOT_REGION_OWNER,
        INVITE_ALREADY_EXISTS,
        YOURSELF,
        TOO_MANY_REQUESTS
    }

    /***
     * Отправляет информационные сообщения о пришлашении отправителю и получателю
     * @param inviter отправитель
     * @param target получатель
     * @param regionId идентификатор региона
     */
    private void sendInviteMessages(Player inviter, Player target, UUID regionId) {
        String messageToTarget = messagesConfig.getString("messages.invite_info");
        String messageAcceptButton = messagesConfig.getString("messages.invite_accept_button");
        String messageDenyButton = messagesConfig.getString("messages.invite_deny_button");

        ClickableMessage clickableMessageToTarget = ClickableMessage.of(messageToTarget);

        clickableMessageToTarget
                .appendButton("&a&l[Принять]", messageAcceptButton, new RunnableAction(() -> {
                    acceptInvite(target, regionId);
                }))
                .appendText(" ")
                .appendButton("&c&l[Отклонить]", messageDenyButton, new RunnableAction(() -> {
                    declineInvite(target, regionId);
                })).sendTo(target);
    }

    /**
     * Отправляет приглашение игроку
     * @param inviter отправитель
     * @param target получатель
     * @param regionId идентификатор региона
     * @return результат отправки
     */
    public InviteResult sendInvite(Player inviter, Player target, UUID regionId) {
        Region region = regionManager.getRegion(regionId);

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            return InviteResult.YOURSELF;
        }

        if (region == null) {
            return InviteResult.REGION_NOT_FOUND;
        }

        if (!regionManager.isOwner(region, inviter.getUniqueId())) {
            return InviteResult.NOT_REGION_OWNER;
        }

        if (requestManager.findRequest(target.getUniqueId(), regionId) != null) {
            return InviteResult.INVITE_ALREADY_EXISTS;
        }

        if (!requestManager.canSendInvite(inviter.getUniqueId())) {
            return InviteResult.TOO_MANY_REQUESTS;
        }

        sendInviteMessages(inviter, target, regionId);
        requestManager.addRequest(inviter, target, regionId);
        return InviteResult.SUCCESS;
    }

    /**
     * Принимает приглашение
     * @param target игрок, принимающий приглашение
     * @param regionId идентификатор региона
     * @return результат принятия
     */
    public boolean acceptInvite(Player target, UUID regionId) {
        UUID targetUuid = target.getUniqueId();
        Request request = requestManager.findRequest(targetUuid, regionId);
        if (request == null) {
            return false;
        }

        UUID inviter = request.inviter();

        RequestAcceptedEvent requestAcceptedEvent = new RequestAcceptedEvent(inviter, targetUuid, regionId);
        Bukkit.getPluginManager().callEvent(requestAcceptedEvent);

        Player inviterPlayer = Bukkit.getPlayer(inviter);
        boolean inviterIsNull = inviterPlayer == null;

        UniversalMessenger.send(target, messagesConfig.getString("messages.invite_accepted",
                new ReplaceData("{player}", inviterIsNull ? "Offline" : inviterPlayer.getName())));
        if (!inviterIsNull) {
            UniversalMessenger.send(inviterPlayer, messagesConfig.getString("messages.invite_accepted",
                new ReplaceData("{player}", target.getName())));
        }

        requestManager.removeRequest(request);
        return true;
    }

    /**
     * Отклоняет приглашение
     * @param target игрок, отклоняющий приглашение
     * @param regionId идентификатор региона
     * @return результат отклонения
     */
    public boolean declineInvite(Player target, UUID regionId) {
        UUID targetUuid = target.getUniqueId();
        Request request = requestManager.findRequest(targetUuid, regionId);
        if (request == null) {
            return false;
        }

        UUID inviter = request.inviter();
        Player invitPlayer = Bukkit.getPlayer(inviter);

        UniversalMessenger.send(target, messagesConfig.getString("messages.invite_denied",
                new ReplaceData("{player}", invitPlayer.getName())));
        UniversalMessenger.send(invitPlayer, messagesConfig.getString("messages.invite_denied",
                new ReplaceData("{player}", target.getName())));

        requestManager.removeRequest(request);
        return true;
    }
}
