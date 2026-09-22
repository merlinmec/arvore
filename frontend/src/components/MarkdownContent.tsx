import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export function MarkdownContent({ children }: { children: string }) {
  return (
    <div className="prose prose-sm prose-invert mt-1 max-w-none prose-p:my-1 prose-headings:my-2">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  )
}
