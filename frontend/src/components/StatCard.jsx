import { ArrowDownRight, ArrowUpRight } from 'lucide-react'

export default function StatCard({ label, value, change, icon: Icon, tone = 'sage', hint }) {
  const positive = !String(change || '').startsWith('-')
  return <article className="stat-card"><div className={`stat-icon ${tone}`}>{Icon && <Icon size={20} strokeWidth={1.8} />}</div><span className="stat-label">{label}</span><strong className="stat-value">{value}</strong><div className="stat-foot"><span className={positive ? 'trend positive' : 'trend negative'}>{positive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}{change}</span><span>{hint || 'vs last month'}</span></div></article>
}
