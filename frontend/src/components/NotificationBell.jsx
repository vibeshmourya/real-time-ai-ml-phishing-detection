import React, { useState, useEffect, useRef } from 'react';
import { 
  Bell, BellOff, Check, CheckCheck, Trash2, ShieldAlert, 
  ExternalLink, Settings, Volume2, Wifi, WifiOff, X, AlertTriangle, ShieldCheck
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import notificationApi from '../api/notificationApi';

export default function NotificationBell({ setActiveTab }) {
  const { isAuthenticated } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [activeFilter, setActiveFilter] = useState('all'); // 'all' | 'unread'
  const [isSseConnected, setIsSseConnected] = useState(false);
  const [preferencesOpen, setPreferencesOpen] = useState(false);
  const [preferences, setPreferences] = useState({
    inAppEnabled: true,
    browserEnabled: false,
    emailEnabled: false,
    minSeverity: 'MEDIUM',
    scanAlertsEnabled: true,
    incidentAlertsEnabled: true,
    systemAdvisoriesEnabled: true
  });
  const [prefSaving, setPrefSaving] = useState(false);

  const dropdownRef = useRef(null);
  const eventSourceRef = useRef(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch initial unread count on login
  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setNotifications([]);
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      return;
    }

    fetchUnreadCount();
    fetchPreferences();

    // Setup Real-time SSE Connection
    const token = localStorage.getItem('phishshield_token');
    if (token) {
      connectEventSource(token);
    }

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [isAuthenticated]);

  const connectEventSource = (token) => {
    try {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }

      const es = notificationApi.createEventSource(token);
      eventSourceRef.current = es;

      es.addEventListener('CONNECTED', () => {
        setIsSseConnected(true);
      });

      es.addEventListener('NOTIFICATION', (event) => {
        try {
          const newAlert = JSON.parse(event.data);
          setNotifications((prev) => [newAlert, ...prev]);
          setUnreadCount((prev) => prev + 1);

          // Browser Web Notification if allowed
          if (Notification.permission === 'granted' && preferences.browserEnabled) {
            new Notification(newAlert.title, {
              body: newAlert.message,
              icon: '/favicon.ico'
            });
          }
        } catch (e) {
          console.error('Failed to parse SSE notification payload', e);
        }
      });

      es.addEventListener('PING', () => {
        // Keep-alive heartbeat received
      });

      es.onerror = () => {
        setIsSseConnected(false);
      };
    } catch (err) {
      console.warn('Could not establish SSE stream', err);
      setIsSseConnected(false);
    }
  };

  const fetchUnreadCount = async () => {
    try {
      const data = await notificationApi.getUnreadCount();
      setUnreadCount(data.unreadCount || 0);
    } catch (err) {
      console.error('Failed to fetch unread count', err);
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await notificationApi.getNotifications({
        unreadOnly: activeFilter === 'unread',
        page: 0,
        size: 15
      });
      setNotifications(res.content || []);
    } catch (err) {
      console.error('Failed to load notifications', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchPreferences = async () => {
    try {
      const data = await notificationApi.getPreferences();
      setPreferences(data);
    } catch (err) {
      console.error('Failed to fetch notification preferences', err);
    }
  };

  const handleToggleOpen = () => {
    if (!isOpen) {
      fetchNotifications();
    }
    setIsOpen(!isOpen);
  };

  const handleMarkAsRead = async (id, e) => {
    e?.stopPropagation();
    try {
      const updated = await notificationApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: updated.readAt } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Failed to mark alert as read', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read', err);
    }
  };

  const handleDeleteNotification = async (id, e) => {
    e?.stopPropagation();
    try {
      await notificationApi.deleteNotification(id);
      const target = notifications.find((n) => n.id === id);
      if (target && !target.isRead) {
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
      setNotifications((prev) => prev.filter((n) => n.id !== id));
    } catch (err) {
      console.error('Failed to delete alert', err);
    }
  };

  const handleSavePreferences = async () => {
    setPrefSaving(true);
    try {
      // Prompt for browser notification permission if user turned it on
      if (preferences.browserEnabled && Notification.permission !== 'granted') {
        const permission = await Notification.requestPermission();
        if (permission !== 'granted') {
          preferences.browserEnabled = false;
        }
      }

      const updated = await notificationApi.updatePreferences(preferences);
      setPreferences(updated);
      setPreferencesOpen(false);
    } catch (err) {
      console.error('Failed to update notification preferences', err);
    } finally {
      setPrefSaving(false);
    }
  };

  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-100 text-red-700 dark:bg-red-950/60 dark:text-red-400 border-red-200 dark:border-red-800';
      case 'HIGH':
        return 'bg-orange-100 text-orange-700 dark:bg-orange-950/60 dark:text-orange-400 border-orange-200 dark:border-orange-800';
      case 'MEDIUM':
        return 'bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-400 border-amber-200 dark:border-amber-800';
      default:
        return 'bg-blue-100 text-blue-700 dark:bg-blue-950/60 dark:text-blue-400 border-blue-200 dark:border-blue-800';
    }
  };

  if (!isAuthenticated) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Button with Live Badge */}
      <button
        type="button"
        onClick={handleToggleOpen}
        className="relative p-2 rounded-xl text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
        title="Security Alerts & Notifications"
        aria-label="Security Alerts & Notifications"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 px-1.5 py-0.5 min-w-[18px] text-[10px] font-black leading-none bg-red-600 text-white rounded-full flex items-center justify-center animate-pulse shadow-xs">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notifications Drawer / Popover */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-2xl p-0 z-50 overflow-hidden animate-in fade-in">
          
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/75 dark:bg-dark-950/50">
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-slate-900 dark:text-white">Security Alerts</span>
              <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-medium ${isSseConnected ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-400' : 'bg-slate-200 text-slate-600 dark:bg-slate-800 dark:text-slate-400'}`}>
                {isSseConnected ? <Wifi className="w-2.5 h-2.5 mr-1" /> : <WifiOff className="w-2.5 h-2.5 mr-1" />}
                {isSseConnected ? 'Live' : 'Polling'}
              </span>
            </div>

            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setPreferencesOpen(true)}
                className="p-1 text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 rounded-md hover:bg-slate-200/50 dark:hover:bg-slate-800 transition-colors"
                title="Alert Preferences"
              >
                <Settings className="w-3.5 h-3.5" />
              </button>
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllAsRead}
                  className="text-[11px] font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 flex items-center gap-1 ml-1"
                  title="Mark all as read"
                >
                  <CheckCheck className="w-3 h-3" /> Mark Read
                </button>
              )}
            </div>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-2 px-4 py-2 border-b border-slate-100 dark:border-slate-800 text-xs">
            <button
              onClick={() => { setActiveFilter('all'); fetchNotifications(); }}
              className={`px-2.5 py-1 rounded-lg font-medium transition-colors ${activeFilter === 'all' ? 'bg-blue-600 text-white' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
            >
              All Alerts
            </button>
            <button
              onClick={() => { setActiveFilter('unread'); fetchNotifications(); }}
              className={`px-2.5 py-1 rounded-lg font-medium transition-colors ${activeFilter === 'unread' ? 'bg-blue-600 text-white' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
            >
              Unread ({unreadCount})
            </button>
          </div>

          {/* Notification List Body */}
          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-800/60">
            {loading ? (
              <div className="p-6 text-center text-xs text-slate-500">Loading alerts...</div>
            ) : notifications.length === 0 ? (
              <div className="p-8 text-center">
                <ShieldCheck className="w-8 h-8 text-emerald-500 mx-auto mb-2 opacity-80" />
                <p className="text-xs font-semibold text-slate-800 dark:text-slate-200">No security alerts</p>
                <p className="text-[11px] text-slate-400 mt-0.5">Your accounts and scans are healthy.</p>
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                  className={`p-3.5 transition-colors cursor-pointer group hover:bg-slate-50 dark:hover:bg-slate-800/40 ${!n.isRead ? 'bg-blue-50/40 dark:bg-blue-950/20' : ''}`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <span className={`text-[10px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded border ${getSeverityBadge(n.severity)}`}>
                        {n.severity}
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        {n.notificationType?.replace('_', ' ')}
                      </span>
                    </div>

                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {!n.isRead && (
                        <button
                          type="button"
                          onClick={(e) => handleMarkAsRead(n.id, e)}
                          className="p-1 text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 rounded"
                          title="Mark as read"
                        >
                          <Check className="w-3.5 h-3.5" />
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={(e) => handleDeleteNotification(n.id, e)}
                        className="p-1 text-slate-400 hover:text-red-600 dark:hover:text-red-400 rounded"
                        title="Delete alert"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  <h4 className="text-xs font-bold text-slate-900 dark:text-white mt-1.5 line-clamp-1">
                    {n.title}
                  </h4>
                  <p className="text-[11px] text-slate-600 dark:text-slate-400 mt-1 line-clamp-2 leading-relaxed">
                    {n.message}
                  </p>

                  {/* Deep link to scan or incident */}
                  {(n.relatedScanId || n.relatedIncidentId) && (
                    <div className="mt-2 flex items-center gap-2">
                      {n.relatedScanId && (
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTab('history');
                            setIsOpen(false);
                          }}
                          className="inline-flex items-center text-[11px] font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400"
                        >
                          View Scan Result <ExternalLink className="w-3 h-3 ml-0.5" />
                        </button>
                      )}
                      {n.relatedIncidentId && (
                        <button
                          type="button"
                          onClick={() => {
                            setActiveTab('safety');
                            setIsOpen(false);
                          }}
                          className="inline-flex items-center text-[11px] font-semibold text-amber-600 hover:text-amber-700 dark:text-amber-400"
                        >
                          Open Safety Centre <ExternalLink className="w-3 h-3 ml-0.5" />
                        </button>
                      )}
                    </div>
                  )}

                  <div className="text-[10px] text-slate-400 mt-2 font-mono">
                    {new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • {new Date(n.createdAt).toLocaleDateString()}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Preferences Modal Dialog */}
      {preferencesOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white dark:bg-dark-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full shadow-2xl overflow-hidden p-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
              <h3 className="font-bold text-slate-900 dark:text-white text-base">Alert Preferences</h3>
              <button onClick={() => setPreferencesOpen(false)} className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-4 py-4 text-xs">
              {/* Delivery Channels */}
              <div>
                <label className="font-bold text-slate-700 dark:text-slate-300 block mb-2">Delivery Channels</label>
                <div className="space-y-2">
                  <label className="flex items-center justify-between p-2 rounded-lg bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 cursor-pointer">
                    <span className="text-slate-700 dark:text-slate-300">In-App Notification Center</span>
                    <input
                      type="checkbox"
                      checked={preferences.inAppEnabled}
                      onChange={(e) => setPreferences({ ...preferences, inAppEnabled: e.target.checked })}
                      className="rounded text-blue-600"
                    />
                  </label>
                  <label className="flex items-center justify-between p-2 rounded-lg bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 cursor-pointer">
                    <span className="text-slate-700 dark:text-slate-300">Browser Desktop Notifications</span>
                    <input
                      type="checkbox"
                      checked={preferences.browserEnabled}
                      onChange={(e) => setPreferences({ ...preferences, browserEnabled: e.target.checked })}
                      className="rounded text-blue-600"
                    />
                  </label>
                </div>
              </div>

              {/* Minimum Severity Threshold */}
              <div>
                <label className="font-bold text-slate-700 dark:text-slate-300 block mb-1.5">Minimum Severity Alert</label>
                <select
                  value={preferences.minSeverity}
                  onChange={(e) => setPreferences({ ...preferences, minSeverity: e.target.value })}
                  className="w-full p-2 rounded-lg bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-200"
                >
                  <option value="INFO">INFO & Above (All Events)</option>
                  <option value="MEDIUM">MEDIUM & Above (Recommended)</option>
                  <option value="HIGH">HIGH & Above (Threats Only)</option>
                  <option value="CRITICAL">CRITICAL Only (Severe Attacks)</option>
                </select>
              </div>

              {/* Categories */}
              <div>
                <label className="font-bold text-slate-700 dark:text-slate-300 block mb-2">Subscribed Event Types</label>
                <div className="grid grid-cols-2 gap-2">
                  <label className="flex items-center gap-2 p-2 rounded-lg bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={preferences.scanAlertsEnabled}
                      onChange={(e) => setPreferences({ ...preferences, scanAlertsEnabled: e.target.checked })}
                      className="rounded text-blue-600"
                    />
                    <span className="text-slate-700 dark:text-slate-300">Scan Threats</span>
                  </label>
                  <label className="flex items-center gap-2 p-2 rounded-lg bg-slate-50 dark:bg-dark-950 border border-slate-200 dark:border-slate-800 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={preferences.incidentAlertsEnabled}
                      onChange={(e) => setPreferences({ ...preferences, incidentAlertsEnabled: e.target.checked })}
                      className="rounded text-blue-600"
                    />
                    <span className="text-slate-700 dark:text-slate-300">Fraud Incidents</span>
                  </label>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setPreferencesOpen(false)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs font-semibold text-slate-700 dark:text-slate-300"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={prefSaving}
                onClick={handleSavePreferences}
                className="px-4 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold"
              >
                {prefSaving ? 'Saving...' : 'Save Settings'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
